package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.AcceptRequest;
import com.chatting.backend.dto.websocket.outbound.*;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.service.UserConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

/**
 * [채팅 초대 수락 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "ACCEPT_REQUEST" 요청을 처리하는 클래스
 * - 즉, 누군가 나를 채팅방에 초대했을 때, 내가 "수락" 버튼을 눌러 서버로 요청을 보내면
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class AcceptRequestHandler implements BaseRequestHandler<AcceptRequest> {

    private final UserConnectionService userConnectionService;
    private final ClientNotificationService clientNotificationService; // 메시지를 보낼 때, "웹소켓 전송 or 푸시 알림"을 자동 분기 처리하는 서비스


    /**
     * 요청 흐름 설명:
     * 1. Inviter(초대한 사람) > 서버  : "이 사용자와 연결하고 싶어"(InviterRequest)
     * 2. 서버 > Acceptor(수락자)     : 초대 알림 전송(InviteNotification)
     * 3. Acceptor(수락자) > 서버     : "좋아, 수락할게"(AcceptRequest)
     * 4. 서버 > 양쪽(Inviter;초대자, Acceptor;수락자)에게 알림 전송
     *      - Acceptor에게는 AcceptResponse를.
     *      - Inviter에게는 AcceptNotifiaction을.
     *
     * @param senderSession 수락자의 세션
     * @param request       AcceptRequest DTO(수락자가 보낸 데이터)
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, AcceptRequest request){
        // 1) 요청자(수락자;Acceptor)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) UserConnectionService를 통해 "수락 처리" 수행
        // 반환값: Pair<Optional<inviterUserId>, String>
        //   - 성공 시: first = 초대한 사람의 userId, second = 수락자의 username
        //   - 실패 시: first = Optional.empty(), second = 에러 메시지
        Pair<Optional<UserId>, String> result = userConnectionService.accept(senderUserId, request.getUsername());

        // 3) 성공 / 실패 분기 처리
        result.getFirst().ifPresentOrElse(inviterUserId -> {
            // === 성공 케이스 ===
            String acceptorUsername = result.getSecond();

            // 3-1) Acceptor(수락자)에게 응답 보내기 : "너가 초대를 수락했어"라는 응답 전송
            clientNotificationService.sendMessage(senderSession, senderUserId, new AcceptResponse(request.getUsername()));
            // 3-2) Inviter(초대자)에게 알림 전송    : "상대방이 너의 초대를 수락했어" 라는 알림 전송
            clientNotificationService.sendMessage(inviterUserId, new AcceptNotification(acceptorUsername));
        }, () -> {
            // === 실패 케이스 ===
            String errorMessage = result.getSecond();

            // 수락자에게 수락하는데 실패했다는 에러 메시지 전송
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.ACCEPT_REQUEST, errorMessage));
        });
    }
}
