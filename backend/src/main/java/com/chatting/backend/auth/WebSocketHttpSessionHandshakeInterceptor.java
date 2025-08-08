package com.chatting.backend.auth;

import com.chatting.backend.constants.Constants;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 연결 전에 HTTP 세션 정보를 WebSocket 세션으로 넘겨주는 인터셉터
 * WebSocket이 연결될 때 기존 로그인 상태(세션)을 가져오기 위함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHttpSessionHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    /**
     * WebSocket 연결이 이루어지기 "직전"에 호출되는 메서드
     * 이 시점에서는 아직 HttpSession이 유효하다
     * 따라서 HttpSession 정보를 WebSocket 세션으로 전달할 수 있는 "잠깐의 순간"이다.
     *
     * 이 메서드를 통해 HttpSession의 ID를 WebSocket 세션 속성(attributes)에 저장해
     * 나중에 WebSocket 세션에서도 HttpSession ID를 사용할 수 있게 한다.
     */
    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,          // 들어온 HTTP 요청
            @NonNull ServerHttpResponse response,        // 응답 객체
            @NonNull WebSocketHandler wsHandler,         // WebSocket 핸들러
            @NonNull Map<String, Object> attributes      // WebSocket 세션과 연결되는 속성 Map (여기에 우리가 데이터를 저장할 것)
    ) throws Exception {

        //1. ServerHttpRequest가 실제 Servlet 기반의 요청인지 확인
        if(request instanceof ServletServerHttpRequest servletServerHttpRequest){

            //2.  HttpSession을 가져온다 (false: 기존 세션이 없으면 새로 생성하지 않음)
            HttpSession httpSession = servletServerHttpRequest.getServletRequest().getSession(false);
            System.out.println("[interceptor] beforeHandshake method의 HttpSession: " + httpSession);

            //3. HttpSession이 존재한다면
            if(httpSession != null){
                //4. WebSocket 세션 속성 Map에 HttpSession의 ID를 저장
                //이로써 WebSocket 세션에서도 이 ID를 통해 HttpSession에 접근할 수 있음
                attributes.put(Constants.HTTP_SESSION_ID.getValue(), httpSession.getId());

                //5. 핸드세이크를 허용
                return true;
            }else{
                //6. HttpSession이 없는 경우(로그인이 안되어 있는 상태일 수 있음)
                log.info("WebSocket handshake failed. httpSession is null");

                //7. 응답 상태를 401 Unauthorized로 설정
                response.setStatusCode(HttpStatus.UNAUTHORIZED);

                //8. 핸드세이크 거부
                return false;
            }
        }else{
            //9. 요청이 Servlet 기반이 아닌 경우: 예상하지 못한 요청이므로 로그 출력
            log.info("WebSocket handshake failed. request is {}", request.getClass());

            //10. 응답 상태를 400 Bad Request로 설정
            response.setStatusCode(HttpStatus.BAD_REQUEST);

            //11. 핸드세이크를 거부
            return false;
        }
    }
}