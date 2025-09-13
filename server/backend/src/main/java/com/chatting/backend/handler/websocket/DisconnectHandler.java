package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.DisConnectRequest;
import com.chatting.backend.dto.websocket.outbound.DisconnectResponse;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.service.UserConnectionService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class DisconnectHandler implements BaseRequestHandler<DisConnectRequest> {

    private final UserConnectionService userConnectionService;
    private final WebSocketSessionManager webSocketSessionManager;

    /**
     * 여기서 요청을 보낸 사람 = 연결 끊고 싶은 사람. 즉, senderSession = 연결 끊는 사람
     *
     * @param senderSession 연결 끊기를 요청하는 사람
     * @param request       해당 request에는 username이 들어있다.
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, DisConnectRequest request) {

        //1) 세션에서 userId 꺼내기 (세션에 userId가 저장되어 있어야 함)
        // - WebSocket 연결/핸드쉐이크 단계나 로그인 과정에서
        //   senderSession.getAttributes().put(IdKey.USER_ID.getValue(), userId)
        //   와 같은 식으로 세션에 UserId가 저장되어 있어야 한다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        Pair<Boolean, String> result = userConnectionService.disconnect(senderUserId, request.getUsername());

        if (result.getFirst()) {
            webSocketSessionManager.sendMessage(senderSession, new DisconnectResponse(request.getUsername(), UserConnectionStatus.DISCONNECTED));
        } else {
            String errorMessage = result.getSecond();
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.DISCONNECT_REQUEST, errorMessage));
        }
    }
}
