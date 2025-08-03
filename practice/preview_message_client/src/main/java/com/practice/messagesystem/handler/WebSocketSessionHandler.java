package com.practice.messagesystem.handler;

import com.practice.messagesystem.service.TerminalService;
import com.practice.messagesystem.service.WebSocketService;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

//WebSocket 연결/종료/에러 발생 시 실행되는 콜백
public class WebSocketSessionHandler extends Endpoint {

    private final TerminalService terminalService;
    private final WebSocketService webSocketService;

    //생성자 주입
    public WebSocketSessionHandler(WebSocketService webSocketService, TerminalService terminalService) {
        this.terminalService = terminalService;
        this.webSocketService = webSocketService;
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
        //background에서 게속 keep-alive가 비정상저긍로 꺼졌을 때, 스레드가 계속 돌고 있으면 에러가 계속 출력될 거라서 이 부분을 명시적으로 끊어주기
        webSocketService.closeSession();
        terminalService.printSystemMessage("Connection closed: " + closeReason.getReasonPhrase());
    }
}
