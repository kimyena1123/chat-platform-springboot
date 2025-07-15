package com.practice.messagesystem.handler;

import com.practice.messagesystem.service.TerminalService;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

//WebSocket 연결/종료/에러 발생 시 실행되는 콜백
public class WebSocketSessionHandler extends Endpoint {

    private final TerminalService terminalService;

    //생성자 주입
    public WebSocketSessionHandler(TerminalService terminalService) {
        this.terminalService = terminalService;
    }

    //세션이 연결됐을 때 호출되는 메서드
    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        terminalService.printSystemMessage("WebSocket Connected.");
    }

    //세션에서 에러가 발생했을 때 호출되는 메서드
    @Override
    public void onError(Session session, Throwable thr) {
        terminalService.printSystemMessage("Error: " + thr.getMessage());
    }

    //연결이 종료되었을 때 호출되는 메서드
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        terminalService.printSystemMessage("Connection closed: " + closeReason.getReasonPhrase());
    }
}
