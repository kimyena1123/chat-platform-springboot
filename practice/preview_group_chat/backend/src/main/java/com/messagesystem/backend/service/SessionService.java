package com.messagesystem.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * SessionService 클래스는 로그인한 사용자의 HttpSession 정보를 기반으로
 * 세션의 TTL(Time To Live, 유효시간)을 연장하는 기능을 제공하는 서비스 클래스
 *
 * 이 클래스는 WebSocket 통신 중에도 세션 유지를 위해 TTL을 갱신해줄 필요가 있는 경우 사용된다
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    /**
     * SessionRepository는 스프링 세션(Spring Session)에서 제공하는 인터페이스로 메모리, Redis, JDBC 등 다양한 저장소에서 세션을 관리할 수 있게 해줌
     * 여기서 사용되는 httpSessionRepository는 Redis 기반 저장소일 수 있으며 Session ID로 세션 객체를 조회하거나 수정할 수 있습니다.
     */
    private final SessionRepository<? extends Session> httpSessionRepository;

    /**
     * 이 메서드는 전달받은 httpSessionId를 이용해 세션을 찾고,
     * 해당 세션이 존재한다면 마지막 접근 시간을 현재 시각으로 갱신한다
     *
     * 이는 TTL(Time To Live)을 연장하는 효과를 가져오며,
     * 세션이 만료되지 않도록 "유지(keep-alive)"하는 데 사용된다
     *
     * 예: 사용자가 WebSocket으로만 활동 중이라도 세션이 계속 살아있도록 해줄 수 있음
     *
     * @param httpSessionId 클라이언트의 HttpSession ID (String 타입)
     */
    public void refreshTTL(String httpSessionId){
        // 전달받은 세션 ID로 저장소에서 세션을 찾는다.
        Session httpSession = httpSessionRepository.findById(httpSessionId);
        System.out.println("refreshTTL 메소드의 httpSessionId = " + httpSessionId);
        System.out.println("refreshTTL 메소드의 httpSession = " + httpSession);


        // 세션이 존재하면, 마지막 접근 시간을 현재 시각으로 갱신한다.
        if(httpSession != null){
            // 현재 시각으로 lastAccessedTime을 갱신함으로써 TTL을 연장한다.
            httpSession.setLastAccessedTime(Instant.now());
        }
        // 만약 세션이 존재하지 않으면 아무것도 하지 않음 (예: 세션이 만료됐거나 잘못된 ID)
    }

    //username을 리턴
    public String getUsername(){
        //현재 연결되어 있는 세션, 내 세션에서 내 이름(username)이 필요한 거라서 security의 도움을 받을 수 있음
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return username;
    }
}