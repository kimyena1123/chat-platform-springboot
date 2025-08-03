package com.practice.messagesystem.handler;

import com.practice.messagesystem.dto.websocket.outbound.MessageRequest;
import com.practice.messagesystem.json.JsonUtil;
import com.practice.messagesystem.service.TerminalService;
import jakarta.websocket.Session;

import java.io.IOException;

//클라이언트에서 서버로 메시지를 전송하는 역할
public class WebSocketSender {

    private final TerminalService terminalService;

    public WebSocketSender(TerminalService terminalService) {
        this.terminalService = terminalService;
    }

    public void sendMessage(Session session, MessageRequest message) {

        if (session != null && session.isOpen()) {
            JsonUtil.toJson(message)
                    .ifPresent(
                            payload ->
                                    session.getAsyncRemote().sendText(payload, result -> {
                                                if (!result.isOK()) {
                                                    terminalService.printSystemMessage("'%s' send failed. cause: %s".formatted(payload, result.getException()));
                                                }
                                            })
                    );
        }
    }
}
