package com.chatting.backend.config;

import com.chatting.backend.auth.WebSocketHttpSessionHandshakeInterceptor;
import com.chatting.backend.handler.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketHandlerConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final WebSocketHttpSessionHandshakeInterceptor webSocketHttpSessionHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(webSocketHandler, "/ws/v1/message")
                .addInterceptors(webSocketHttpSessionHandshakeInterceptor);
    }
}
