package com.practice.preview_spring_security.auth;

import com.practice.preview_spring_security.session.HttpSessionRepository;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * HttpSessionHandshakeInterceptor: WebSocket 핸드셰이크 시 기존 HTTP 세션 속성들을 WebSocket 세션으로 복사해주는 스프링 기본 클래스
 * WebSocket 연결 시 기존의 HTTP tptus(HttpSession)을 WebSocket 세션으로 옮기기 위해서
 * <p>
 * WebSocket은 한 번 연결되면 HTTP 프로토콜에서 벗어나 독립적인 연결이 됨.
 * 따라서 HttpServletRequest, HttpSession 같은 기존 웹 개념은 사용 불가.
 * BUT, 로그인 정보 같은 건 WebSocket에서도 필요하다. 그래서 HTTP 세션 정보를 WebSocket 세션으로 "복사"해줘야 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomHttpSessionHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    private final HttpSessionRepository httpSessionRepository;

    /**
     * 웹소켓 연결하기 전에 Handshake 단계에서 이 메서드가 호출된다.
     * true return: 계속 진행
     * false return: 중단된다(접속 거부되는 것임)
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        List<String> cookies = request.getHeaders().get("Cookie");

        if (cookies != null) { //null이 아니면 하나씩 꺼내서
            for (String cookie : cookies) {
                if (cookie.contains("JSESSIONID")) {
                    String sessionId = cookie.split("=")[1];
                    HttpSession httpSession = httpSessionRepository.findById(sessionId);

                    if (httpSession != null) { //null이 아니면 유효한 세션이 있다는 뜻
                        log.info("Connected sessionId : {}", sessionId);

                        return true;
                    }
                }
            }
        }
        //cookie가 없거나 sessionId를 못찾았거나
        log.info("Unauthorized access attempt : ClientIP = {}", request.getRemoteAddress());
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }
}
