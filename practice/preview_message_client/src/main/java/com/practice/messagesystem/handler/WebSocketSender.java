package com.practice.messagesystem.handler;

import com.practice.messagesystem.dto.Message;
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

    public void sendMessage(Session session, Message message) {
        if (session != null && session.isOpen()) {
            JsonUtil.toJson(message)
                    .ifPresent(
                            msg -> {
                                try {
                                    session.getBasicRemote().sendText(msg);
                                } catch (IOException ex) {
                                    terminalService.printSystemMessage(
                                            String.format("%s send failed: %s", message, ex.getMessage()));
                                }
                            });
        }
    }
}
