package com.chatting.backend.service;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.outbound.BaseMessage;
import com.chatting.backend.entity.MessageEntity;
import com.chatting.backend.json.JsonUtil;
import com.chatting.backend.repository.MessageRepository;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * [메시지 전송 핵심 서비스]
 *
 * 역할 요약
 *  1) 사용자가 보낸 채팅 메시지를 **DB에 우선 저장**한다. (저장 실패 시 실시간 전파도 하지 않음 → 데이터/알림 불일치 방지)
 *  2) 같은 채널에 속한 사용자들 중, **현재 그 채널 화면을 보고 있는 사용자(online)** 를 골라
 *     - WebSocket 세션이 살아 있으면: **웹소켓으로 바로 전달**
 *     - 세션이 없거나 전송 실패하면: **푸시(Fallback)로 전달**
 *
 * 동시성(성능) 포인트
 *  - 여러 수신자에게 동시에 전송하기 위해 `CompletableFuture.runAsync(...)` 를 사용해 **비동기 병렬 전송**.
 *  - (주의) 아래 코드에서는 `runAsync`에 **Executor를 넘기지 않았으므로** 기본적으로 **ForkJoinPool.commonPool**을 사용한다.
 *    → 만약 **고정 크기 스레드 풀(senderThreadPool)** 을 쓰려면 `runAsync(runnable, senderThreadPool)` 형태로 넘기면 된다.
 *    → 스레드 풀 관리는 보통 Spring의 `ThreadPoolTaskExecutor`를 Bean으로 등록해 사용하는 것을 권장.
 */
@Slf4j
@Service
public class MessageService {

//    고정 크기 스레드 풀. 현재 코드는 runAsync에 Executor를 넘기지 않으므로 이 풀은 사용되지 않는다.
//     → commonPool이 아닌 이 풀을 쓰고 싶다면 runAsync 호출부를 runAsync(..., senderThreadPool)로 바꿔야 함.
    private static final int THREAD_POOL_SIZE = 10; // thread pool 만들기

    private final ChannelService channelService;
    private final PushService pushService;
    private final WebSocketSessionManager webSocketSessionManager;
    private final JsonUtil jsonUtil;
    private final MessageRepository messageRepository;
    private final ExecutorService senderThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);


    public MessageService(ChannelService channelService, PushService pushService, WebSocketSessionManager webSocketSessionManager, JsonUtil jsonUtil, MessageRepository messageRepository) {
        this.channelService = channelService;
        this.pushService = pushService;
        this.webSocketSessionManager = webSocketSessionManager;
        this.jsonUtil = jsonUtil;
        this.messageRepository = messageRepository;

        pushService.registerPushMessageType(MessageType.NOTIFY_MESSAGE);
    }

    /**
     * [메시지 전송 엔드포인트]
     *
     * @param senderUserId  보낸 사람(나)의 UserId
     * @param content       실제 텍스트 콘텐츠 (DB 저장용)
     * @param channelId     메시지를 보낼 채널
     * @param message       클라이언트로 보낼 DTO(BaseMessage). 이걸 JSON으로 직렬화해 웹소켓/푸시로 보냄
     *
     * 처리 순서
     *  (A) 전송 Payload 준비: BaseMessage를 JSON으로 변환. 실패하면 즉시 중단(전파 불가).
     *  (B) DB 저장: MessageEntity(senderId, content). 실패하면 실시간 전파도 중단(일관성 유지).
     *  (C) 수신자 선정:
     *      1. 채널 전체 참여자(allParticipantIds) 조회 (DB 기준, 온라인/오프라인 모두 포함)
     *      2. 온라인 참여자 리스트(onlineParticipantIds) 조회
     *         - **중요**: onlineParticipantIds는 allParticipantIds와 **인덱스가 1:1로 매칭**되며,
     *           해당 인덱스가 온라인이면 UserId, 아니면 **null**이 들어 있는 **같은 길이의 리스트**라고 가정한다.
     *  (D) 실제 전송:
     *      - for(int idx ...) 루프에서 인덱스를 기준으로 두 리스트를 나란히 접근한다.
     *      - 보낸 사람(sender) 본인은 건너뛴다.
     *      - onlineParticipantIds[idx] != null 이면: 온라인 → 비동기(runAsync)로 웹소켓 전송 시도.
     *          · 세션이 있으면 웹소켓 전송
     *          · 세션이 없거나 예외 발생 시 푸시로 Fallback
     *      - onlineParticipantIds[idx] == null 이면: 오프라인 → 바로 푸시 전송
     *
     * 설계상의 주의
     *  - 푸시에 사용할 messageType은 PushService에 **사전에 등록**되어 있어야 실제로 푸시가 나간다.
     *    여기서는 `MessageType.NOTIFY_MESSAGE`를 사용하므로, PushService에 이 타입이 등록되어 있어야 함.
     */
    public void sendMessage(UserId senderUserId, String content, ChannelId channelId, BaseMessage message){
        // (A) 직렬화: 서버가 클라이언트로 보낼 DTO(BaseMessage)를 JSON 문자열로 만든다.
        Optional<String> json = jsonUtil.toJson(message);

        if (json.isEmpty()) { // JSON 변환 실패: 전송 자체가 불가능하므로 종료
            log.error("Send message failed. messageType: {}", message.getType());
            return;
        }

        String payload = json.get(); // 실제 전송에 사용할 문자열

        // (B) DB 저장: 메시지를 먼저 저장한다. (이 단계 실패 시 실시간 전파도 중단)
        try{
            // 현재 MessageEntity는 (senderUserId, content)만 저장. (채널ID/메시지타입 등 확장은 Entity에 필드가 생기면 추가)
            messageRepository.save(new MessageEntity(senderUserId.id(), content));
        }catch (Exception ex){
            log.error("Send message failed. cause: {}", ex.getMessage());
            return; // 저장 실패 → 이후 전파 중단(데이터/알림 일관성 보존)
        }


        // (C-1) 채널 참여자 전체 조회 (DB 기준)
        //       이 리스트는 온라인/오프라인을 모두 포함한다. (전달 대상의 "모집단")
        List<UserId> allParticipantIds = channelService.getParticipantIds(channelId);

        // (C-2) 온라인 사용자 리스트 조회
        //       반환 규약: allParticipantIds와 **같은 길이**, 같은 인덱스가 온라인이면 UserId, 아니면 null
        List<UserId> onlineParticipantIds = channelService.getOnlineParticipantIds(channelId, allParticipantIds);

        // (D) 인덱스 정렬을 유지한 채로, 각 참여자에게 전송을 시도한다.
        for(int idx = 0; idx < allParticipantIds.size(); idx++){
            UserId participantId = allParticipantIds.get(idx);

            // (D-0) 보낸 사람(나)은 제외: 내가 보낸 메시지를 나에게 다시 뿌릴 필요가 없다면 건너뜀
            if(senderUserId.equals(participantId)){
                continue;
            }

            // (D-1) 온라인 여부 판단: onlineParticipantIds[idx] != null 이면 "해당 인덱스의 참여자는 현재 이 채널을 보고 있음"
            if(onlineParticipantIds.get(idx) != null){
                // 온라인 → 비동기 전송 (현재는 commonPool 사용; senderThreadPool 사용하려면 두 번째 인자로 넘겨야 함)
                CompletableFuture.runAsync(()-> {
                    try{
                        // (1) 해당 참여자의 웹소켓 세션을 조회
                        WebSocketSession session = webSocketSessionManager.getSession(participantId);

                        // (2) 세션이 살아 있으면 웹소켓으로 즉시 전송
                        if(session != null){
                            webSocketSessionManager.sendMessage(session, payload);
                        }else{
                            // (3) 세션이 없으면 푸시로 Fallback
                            //     - messageType은 알림 성격을 나타내는 식별자. 여기서는 "NOTIFY_MESSAGE" 사용.
                            //     - PushService 쪽에서 이 타입이 register되어 있어야 실제로 푸시가 나감.
                            pushService.pushMessage(participantId, MessageType.NOTIFY_MESSAGE, payload);
                        }
                    }catch (Exception ex){
                        // (4) 전송 중 예외가 나면 웹소켓 경로는 포기하고, 푸시로 Fallback
                        pushService.pushMessage(participantId, MessageType.NOTIFY_MESSAGE, payload);
                    }
                }, senderThreadPool);
            }else{
                // (D-2) 오프라인 → 바로 푸시로 전송 (웹소켓 시도 X)
                pushService.pushMessage(participantId, MessageType.NOTIFY_MESSAGE, payload);
            }
        }
    }


}
