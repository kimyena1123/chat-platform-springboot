package com.chatting.backend.auth;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.UserId;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    ) {

        //ServerHttpRequest가 실제 Servlet 기반의 요청인지 확인
        if(request instanceof ServletServerHttpRequest servletServerHttpRequest){
            //현재 세션의 인증정보를 찾을 수 있다.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("beforeHandshake 메소드의 authentication : " + authentication);

            if(authentication == null){ // 찾은 인증정보가 null이라면 인증이 안된 것을 의미
                log.warn("WebSocket handshake failed. authentication is null.");

                //튕겨내기
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;

            }

            //HttpSession을 가져온다 (false: 기존 세션이 없으면 새로 생성하지 않음)
            HttpSession httpSession = servletServerHttpRequest.getServletRequest().getSession(false);
            System.out.println("[interceptor] beforeHandshake method의 HttpSession: " + httpSession);

            if(httpSession == null){//HttpSession이 존재하지 않는다면,
                //HttpSession이 없는 경우(로그인이 안되어 있는 상태일 수 있음)
                log.info("WebSocket handshake failed. httpSession is null");

                //응답 상태를 401 Unauthorized로 설정
                response.setStatusCode(HttpStatus.UNAUTHORIZED);

                //핸드세이크 거부
                return false;
            }

            //USER_ID를 꺼낼려면 UserDeatils에서 꺼내야 한다.
            MessageUserDetails messageUserDetails = (MessageUserDetails) authentication.getPrincipal();

            //HttpSession이 존재한다면
            //WebSocket 세션 속성 Map에 HttpSession의 ID를 저장
            //이로써 WebSocket 세션에서도 이 ID를 통해 HttpSession에 접근할 수 있음
            attributes.put(IdKey.USER_ID.getValue(), new UserId(messageUserDetails.getUserId()));
            attributes.put(IdKey.HTTP_SESSION_ID.getValue(), httpSession.getId());
            // => 그러면 세션 정보에는 user_id와 로그인 했던 httpSessionId 이 두 개가 저장되어 있는 것이다 그러면 attributes는 이와 같은 형태가 된다.
            //attributes = {
            //  "USER_ID"         :  42L
            //  "HTTP_SESSION_ID" : "f4f97b70-9fa6-4916-86e0-bfd1afb7552a",
            //}

            //핸드세이크를 허용
            return true;

        }else{
            //요청이 Servlet 기반이 아닌 경우: 예상하지 못한 요청이므로 로그 출력
            log.info("WebSocket handshake failed. request is {}", request.getClass());

            //응답 상태를 400 Bad Request로 설정
            response.setStatusCode(HttpStatus.BAD_REQUEST);

            //핸드세이크를 거부
            return false;
        }
    }
}