package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.KeepAlive;
import com.chatting.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class KeepAliveHandler implements BaseRequestHandler<KeepAlive> {

    private final SessionService sessionService;

    /** [TTL 연장]
     *
     * @param senderSession sender의 Session(메시지를 보내는 사람의 세션); 메시지 보내는 사람의 ttl 연장
     * @param request
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, KeepAlive request) {
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        sessionService.refreshTTL(senderUserId, (String) senderSession.getAttributes().get(IdKey.HTTP_SESSION_ID.getValue()));
    }
}
