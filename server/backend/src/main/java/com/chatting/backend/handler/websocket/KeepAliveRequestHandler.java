package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.Constants;
import com.chatting.backend.dto.websocket.inbound.KeepAliveRequest;
import com.chatting.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class KeepAliveRequestHandler implements BaseRequestHandler<KeepAliveRequest> {

    private final SessionService sessionService;

    /** [TTL 연장]
     *
     * @param senderSession sender의 Session(메시지를 보내는 사람의 세션); 메시지 보내는 사람의 ttl 연장
     * @param request
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, KeepAliveRequest request) {
        sessionService.refreshTTL((String) senderSession.getAttributes().get(Constants.HTTP_SESSION_ID.getValue()));
    }
}
