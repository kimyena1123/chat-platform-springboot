package com.practice.preview_spring_security.handler;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    /**
     * 접속이 완료됐을 때 호출되는 메서드
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected: {}", session.getId());
    }

    /**
     * 접속이 끊어졌을 때 호출되는 메서드
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        log.info("Disconnected: {}, status: {}", session.getId(), session);
    }
}
