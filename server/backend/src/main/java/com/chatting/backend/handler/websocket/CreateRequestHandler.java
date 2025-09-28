package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.ResultType;
import com.chatting.backend.dto.domain.Channel;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.CreateRequest;
import com.chatting.backend.dto.websocket.outbound.CreateResponse;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.JoinNotification;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * [채널(채팅방) 생성 요청 처리 핸들러] : 클라이언트가 "채팅방 생성"을 요청했을 때 처리하는 WebSocket 핸들러.
 *
 * - 클라이언트가 보낸 "CREATE_REQUEST" 요청을 처리하는 클래스
 * - 채팅방명 + 참여자 username들 입력하면 채널 생성됨
 *
 * 동작 시나리오(카카오톡 비유):
 *  1) 내가 새 단체방을 만들려고 "참여자 username 목록 + 방 제목"을 서버로 보냄(CreateRequest).
 *  2) 서버는 username → userId를 모두 확인한 뒤, 채널 테이블과 채널-사용자 매핑 테이블에 "생성"을 기록(트랜잭션).
 *  3) 성공하면,
 *      - 나(생성자)에게는: "생성 성공" 응답(CreateResponse)을 보냄.
 *      - 초대된 참여자들에게는: "당신은 새 채팅방에 가입되었습니다" 알림(JoinNotification)을 푸시/웹소켓으로 보냄.
 *
 * 설계 포인트:
 *  - 네트워크 세션(WebSocketSession)을 이미 알고 있는 "나 자신"에게는 세션으로 즉시 전송.
 *  - 참여자들에게는 세션을 몰라도 UserId만으로 전송 요청 → ClientNotificationService가
 *    내부에서 세션 조회/존재 시 WebSocket, 부재 시 Push로 자동 분기(오프라인 대응).
 *  - 참여자 브로드캐스트는 비동기(CompletableFuture.runAsync)로 처리하여 핸들러의 응답 지연을 줄임.
 */
@Component
@RequiredArgsConstructor
public class CreateRequestHandler implements BaseRequestHandler<CreateRequest> {

    private final ChannelService channelService;
    private final UserService userService;
    private final ClientNotificationService clientNotificationService; // 메시지를 보낼 때, "웹소켓 전송 or 푸시 알림"을 자동 분기 처리하는 서비스

    /**
     * @param senderSession 채팅방 생성 요청한 사람의 세션
     * @param request       클라이언트가 보낸 생성 요청 DTO (title, participantUsernames 포함)
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, CreateRequest request) {
        // 1) 요청자(채널 생성자)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) 요청에 담긴 여러 username들을 실제 UserId 리스트로 변환(채팅방 참여할 자들의 userId 구하기)
        List<UserId> participantIds = userService.getUserIds(request.getParticipantUsernames());

        // 참여자들의 userId 조회 실패 > 에러 응답 NOT_FOUND 반환
        if (participantIds.isEmpty()) {
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.CREATE_REQUEST, ResultType.NOT_FOUND.getMessage()));
            return;
        }


        // ChannelService에 있는 채널 생성 메서드의 결과를 저장할 변수 선언
        Pair<Optional<Channel>, ResultType> result;

        //transaction으로 잡았기에 에러가 터질 수 있다. 해당 transaction이 있는건 에러 처리 해줘야 함
        try {
            // 3) 채널 생성 트랜잭션 수행 (channel + channel_user 2건 작성)
            result = channelService.create(senderUserId, participantIds, request.getTitle());
        } catch (Exception ex) {
            // 내부 오류 → FAILED
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.CREATE_REQUEST, ResultType.FAILED.getMessage()));
            return; // 실패면 여기서 종료해야 함(아래 성공 흐름을 타지 않도록)
        }

        // Channel 정보가 비어있다면
        if (result.getFirst().isEmpty()) {
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.CREATE_REQUEST, result.getSecond().getMessage()));
        }

        // 4) 채널 생성한 결과를 담을 변수
        Channel channel = result.getFirst().get();

        // 채팅방 개설을 한 후 (최대 100명)
        // 채팅방 개설자(요청자; 1명)에게 응답(CreateResponse)를,
        // 채팅방 참여자(요청받은 자; 99명)에게 알림(JoinNotification)을 전송

        // - 채팅방 생성 요청자에게 보내는 응답
        clientNotificationService.sendMessage(senderSession, senderUserId, new CreateResponse(channel.channelId(), channel.title()));

        // - 채팅방 참여자들에게 보내는 알림
        //     - CompletableFuture.runAsync: 비동기 실행(비동기 작업을 실행하기 위해 사용. 현재 실행 흐름(메인 스레드)을 막지 않고, 별도의 스레드에서 병렬로 실행되도록 한다.)
        //       기본적으로 ForkJoinPool.commonPool을 사용(별도 Executor를 지정하지 않으면)
        //       → 핸들러(메인 스레드)를 오래 잡지 않고, 참여자 수만큼 "병렬로" 전송 작업을 던져 버린다.
        //       참여자가 많을 수 있는 브로드캐스트 성격이므로, 응답 지연을 줄이는 효과가 있다.
        participantIds.forEach(participantId -> CompletableFuture.runAsync(() -> {

            clientNotificationService.sendMessage(participantId, new JoinNotification(channel.channelId(), channel.title()));
            }
        ));


    }
}
