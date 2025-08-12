package com.chatting.backend.handler.websocket;

import com.chatting.backend.dto.websocket.inbound.BaseRequest;
import org.springframework.web.socket.WebSocketSession;

/**
 * 이 interface가 여기에 대응하는 모든 request들, inbound로 들어오는 걸 제너릭으로 처리할 수 있어야 한다
 * InviteRequest, WriteMessageRequest, KeepAliveRequest가 이 T에 들어온다.
 */
public interface BaseRequestHandler<T extends BaseRequest> {


    /** [MessageHandler]의 "handleTextMessage()" 메서드 내용을 분기처리하려고 만든 것.
     *
     * 이 메서드를 호출하면 각 핸들러로 자동 분기되는 걸 기대한다.
     * 실제 이 핸들러를 사용하는 곳에서는 어떤 객체가 어떤 구현체가 연결돼서 호출되는지는 몰라도 되는 것이다.
     *
     * @param webSocketSession 메시지를 보내려고 하는 사람의 세션(채팅을 보내는 자; 현재 이 플랫폼을 사용하는 "나"를 의미)
     * @param request
     */
    void handleRequest(WebSocketSession webSocketSession, T request);
}
