package com.chatting.backend.service;

import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.entity.MessageEntity;
import com.chatting.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * [메시지 보내기 핵심 비즈니스 로직]
 * - 1) 메시지를 DB에 저장하고
 * - 2) 동일 채널에 참여 중이며, 현재 그 채널을 보고 있는 (online) 사용자들에게 실시간 알림(MessageNotification)을 전달한다.
 *
 * 단일 스레드(순차 전송) → 멀티 스레드(병렬 전송)로 확장:
 * - 그룹 채팅: 수십/백 명에게 한 번에 보내야 할 수 있음
 * - I/O(웹소켓 전송)는 네트워크 지연이 있으므로 순차 처리 시 전체 전파가 느려짐
 * - 스레드 풀로 병렬 실행하면 전체 전파 지연을 줄일 수 있음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    /**
     * [스레드 풀 설정]
     * - 고정 크기 스레드 풀(워커 스레드 10개)을 생성
     * - 아래 runAsync(..., senderThreadPool) 에서 이 풀을 사용해 비동기 전송을 실행
     *
     * 주의:
     *  - 실제 서비스에서는 Spring의 ThreadPoolTaskExecutor를 빈으로 등록/주입해서 관리(모니터링/종료)하는 것을 권장
     *  - @PreDestroy로 shutdown 처리도 고려(아래 주석 참고)
     */
    private static final int THREAD_POOL_SIZE = 10; // thread pool 만들기

    private final ChannelService channelService;
    private final MessageRepository messageRepository;
    private final ExecutorService senderThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);


    /**
     * [채팅 메시지를 저장 + 대상자에게 병렬 전송 지시]
     *
     * 동작 순서:
     *  1) DB에 메시지 저장 (영속화가 실패하면 이후 전송 자체를 하지 않음 → 메시지/알림 불일치 방지)
     *  2) 현재 이 채널 화면을 보고 있는 참여자 목록 조회
     *  3) 보낸 사람(나) 제외
     *  4) 각 대상자에 대해 "비동기 전송 작업"을 스레드 풀에 제출(runAsync)
     *
     * 멀티스레딩 포인트:
     *  - 아래 forEach 내부의 runAsync(...) 가 바로 비동기/병렬 실행을 트리거하는 부분
     *  - runAsync는 "현재 스레드에서 바로 전송"하지 않고, senderThreadPool의 워커 스레드가 전송을 수행
     *  - 따라서 여러 수신자 전송이 동시에(최대 THREAD_POOL_SIZE개) 진행될 수 있음
     *
     * @param senderUserId  메시지를 보낸 사용자 ID
     * @param content       전송한 메시지 내용
     * @param channelId     메시지를 전송할 채널 ID
     * @param messageSender 실제 전송 콜백 (ex. 특정 UserId의 WebSocketSession에 메시지 push)
     */
    public void sendMessage(UserId senderUserId, String content, ChannelId channelId, Consumer<UserId> messageSender) {

        try {
            // 1) 메시지 저장 : 누가, 어떤 내용을 보내는지 저장 (반드시 DB에 먼저 저장 (실패 시 이후 전송 중단))
            messageRepository.save(new MessageEntity(senderUserId.id(), content));

        } catch (Exception ex) {
            // 저장 실패면 실시간 전송을 하지 않음(유실/불일치 방지).
            log.error("Send message failed. cause: {}", ex.getMessage());
            return; // 저장이 실패했으므로 전송 작업을 진행하지 않음
        }


        // 2) 이 채널을 '지금 실제로 보고 있는(online)' 참여자들 목록 조회
        //    - 1:1에서는 isOnline(userId, channelId)로 한 명 검사면 됐지만,
        //      그룹에서는 Redis MGET으로 한 번에 걸러내는 최적화를 ChannelService에서 수행하도록 설계되었을 수 있음.
        //    - 여기서는 '이미 online만 걸러서' 반환한다고 가정.
        channelService.getOnlineParticipantIds(channelId).stream() //// List<UserId>
                //3) 참야자들 중에 "나"를 제외하고, 현재 이 채널을 보고있는(현재 채널에서 "활동"중인) 참여자에게만
                .filter(participantId -> !senderUserId.equals(participantId))
                // 4) 각 대상자에 대해 "비동기 전송 작업"을 스레드 풀에 제출
                .forEach(participantId -> CompletableFuture.runAsync(
                        // 실제 I/O 전송 (예: WebSocketSessionManager.sendMessage)
                        () -> messageSender.accept(participantId), senderThreadPool) //senderThreadPool:  여기 때문에 멀티스레딩/병렬 수행이 됨
                );
    }
}
