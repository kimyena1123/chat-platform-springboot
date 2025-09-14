package com.chatting.messageclient.handler;

import com.chatting.messageclient.service.TerminalService;
import com.chatting.messageclient.service.UserService;
import com.chatting.messageclient.service.WebSocketService;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

public class WebSocketSessionHandler extends Endpoint {

    private final UserService userService;
    private final WebSocketService webSocketService;
    private final TerminalService terminalService;

    public WebSocketSessionHandler(UserService userService, WebSocketService webSocketService, TerminalService terminalService) {
        this.userService = userService;
        this.webSocketService = webSocketService;
        this.terminalService = terminalService;
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        terminalService.printSystemMessage("WebSocket Connected.");
    }

    @Override
    public void onError(Session session, Throwable thr) {
        terminalService.printSystemMessage("Error: " + thr.getMessage());
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        userService.logout();
        webSocketService.closeSession();
        terminalService.printSystemMessage("Connection closed: " + closeReason.getReasonPhrase());
    }
}