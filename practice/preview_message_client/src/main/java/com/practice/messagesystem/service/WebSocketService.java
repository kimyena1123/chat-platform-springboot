package com.practice.messagesystem.service;

import com.practice.messagesystem.dto.domain.Message;
import com.practice.messagesystem.dto.websocket.outbound.BaseRequest;
import com.practice.messagesystem.dto.websocket.outbound.KeepAliveRequest;
import com.practice.messagesystem.dto.websocket.outbound.MessageRequest;
import com.practice.messagesystem.handler.WebSocketMessageHandler;
import com.practice.messagesystem.handler.WebSocketSender;
import com.practice.messagesystem.handler.WebSocketSessionHandler;
import com.practice.messagesystem.json.JsonUtil;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//WebSocket 연결, 전송, 종료 등 핵심 로직 담당
public class WebSocketService {

    private final TerminalService terminalService;
    private final WebSocketSender messageSender;
    private final String webSocketUrl;
    private WebSocketMessageHandler webSocketMessageHandler;
    private Session session;

    //쓰레드 처리를 위해서
    private ScheduledExecutorService scheduledExecutorService = null;

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
    public boolean createSession(String sessionId) { //sessionId를 받아서 헤더에 추가해줘야 함
        ClientManager client = ClientManager.createClient();

        ClientEndpointConfig.Configurator configurator = new ClientEndpointConfig.Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                //header 세팅
                headers.put("Cookie", List.of("SESSION=" + sessionId));
            }
        };

        ClientEndpointConfig config = ClientEndpointConfig.Builder.create().configurator(configurator).build();

        try {
            session = client.connectToServer(new WebSocketSessionHandler(this, terminalService), config, new URI(webSocketUrl));
            session.addMessageHandler(webSocketMessageHandler);

            enableKeepAlive();

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
            disableKeepAlive();
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
    public void sendMessage(BaseRequest baseRequest) {
        if(session != null && session.isOpen()){

            if(baseRequest instanceof MessageRequest messageRequest){
                messageSender.sendMessage(session, messageRequest);
                return;
            }

            JsonUtil.toJson(baseRequest)
                    .ifPresent(payload -> session
                            .getAsyncRemote()
                            .sendText(payload, result -> {
                                if(!result.isOK()){
                                    terminalService.printSystemMessage("'%s' send failed. cause: %s".formatted(payload, result.getException()));
                                }
                            }));
        }
    }

    private void enableKeepAlive() {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }
        scheduledExecutorService.scheduleAtFixedRate(() -> sendMessage(new KeepAliveRequest()), 1, 1, TimeUnit.MINUTES);
    }

    private void disableKeepAlive() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
    }
}
