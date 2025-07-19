package com.practice.preview_spring_security.config;

import com.practice.preview_spring_security.auth.CustomHttpSessionHandshakeInterceptor;
import com.practice.preview_spring_security.handler.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor

/** 흐름
 * [클라이언트]
 *      ↓   WebSocket 연결 요청 (ws://localhost:8080/ws/v1/connect)
 *      ↓   Cookie: JSESSIONID=abcd1234 전송
 *
 * [서버: WebSocketConfig]
 *      ↓   해당 요청을 webSocketHandler에 전달
 *      ↓   + 연결 전 인터셉터 호출 (CustomHttpSessionHandshakeInterceptor.beforeHandshake)
 *
 * [서버: CustomHttpSessionHandshakeInterceptor]
 *      ↓   쿠키에서 JSESSIONID 추출
 *      ↓   HttpSessionRepository를 통해 세션 확인
 *      ↓   세션이 있으면 true → 연결 허용
 *          없으면 false → 연결 거부 (401 Unauthorized)
 */
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final CustomHttpSessionHandshakeInterceptor customHttpSessionHandshakeInterceptor;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/v1/connect") //WebSocket 요청 URL이고, 이 URL로 들어오는 요청을 webSocketHandler가 처리함
                .addInterceptors(customHttpSessionHandshakeInterceptor); //그리고 핸드셰이크 전에 customHttpSessionHandshakeInterceptor가 실행됨
    }
}
