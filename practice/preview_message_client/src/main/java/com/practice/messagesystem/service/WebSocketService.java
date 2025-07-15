package com.practice.messagesystem.service;

import com.practice.messagesystem.dto.Message;
import com.practice.messagesystem.handler.WebSocketMessageHandler;
import com.practice.messagesystem.handler.WebSocketSender;
import com.practice.messagesystem.handler.WebSocketSessionHandler;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;

import java.io.IOException;
import java.net.URI;

//WebSocket 연결, 전송, 종료 등 핵심 로직 담당
public class WebSocketService {

    private final TerminalService terminalService;
    private final WebSocketSender messageSender;
    private final String webSocketUrl;
    private WebSocketMessageHandler webSocketMessageHandler;
    private Session session;

    public WebSocketService(TerminalService terminalService, WebSocketSender messageSender, String url, String endpoint) {
        this.terminalService = terminalService;
        this.messageSender = messageSender;
        this.webSocketUrl = "ws://" + url + endpoint;
    }

    //수신 메시지 핸들러 설정
    public void setWebSocketMessageHandler(WebSocketMessageHandler webSocketMessageHandler) {
        this.webSocketMessageHandler = webSocketMessageHandler;
    }

    //서버와의 연결 생성
    public boolean createSession() {
        ClientManager client = ClientManager.createClient();

        try {
            session = client.connectToServer(new WebSocketSessionHandler(terminalService), new URI(webSocketUrl));
            session.addMessageHandler(webSocketMessageHandler);

            return true;
        } catch (Exception ex) {
            terminalService.printSystemMessage(
                    String.format("Failed to connect to [%s] error: %s", webSocketUrl, ex.getMessage()));

            return false;
        }
    }

    //연결 종료
    public void closeSession() {
        try {
            if (session != null) {
                if (session.isOpen()) {
                    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "NORMAL CLOSURE"));
                }

                session = null;
            }
        } catch (IOException ex) {
            terminalService.printSystemMessage(String.format("Failed to close. error: %s", ex.getMessage()));
        }
    }

    //메시지 전송
    public void sendMessage(Message message) {
        if (session != null && session.isOpen()) {
            messageSender.sendMessage(session, message);
        } else {
            terminalService.printSystemMessage("Failed to send message. Session is not open.");
        }
    }
}
