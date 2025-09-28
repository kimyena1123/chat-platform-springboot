package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.DisconnectRequest;
import com.chatting.backend.dto.websocket.outbound.DisconnectResponse;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.service.UserConnectionService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * [(친구) 연결 끊기 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "DISCONNECT_REQUEST" 요청을 처리하는 클래스
 * - 즉, 내가 상대방과의 연결을 "끊고" 싶을 때 서버로 요청을 보내면
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class DisconnectHandler implements BaseRequestHandler<DisconnectRequest> {

    private final UserConnectionService userConnectionService;
    private final ClientNotificationService clientNotificationService;

    /**
     * @param senderSession 연결 끊기를 요청하는 사람
     * @param request       DisconnectRequest DTO
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, DisconnectRequest request) {

        // 1) 요청자(연결끊는 자; disconnector)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        Pair<Boolean, String> result = userConnectionService.disconnect(senderUserId, request.getUsername());

        if (result.getFirst()) {
            // 연결 끊는 자(요청자)에게 응답 보내기 : "너가 상대방과의 연결을 끊었어"라는 응답 전송
            // 상대방은 자신이 연결이 끊겼는지 알 필요 없음(알림 전송X)
            clientNotificationService.sendMessage(senderSession, senderUserId, new DisconnectResponse(request.getUsername(), UserConnectionStatus.DISCONNECTED));
        } else {
            String errorMessage = result.getSecond();
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.DISCONNECT_REQUEST, errorMessage));
        }
    }
}
