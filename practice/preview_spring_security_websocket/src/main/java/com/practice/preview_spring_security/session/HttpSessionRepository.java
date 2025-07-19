package com.practice.preview_spring_security.session;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class HttpSessionRepository implements HttpSessionListener {

    //세션 저장소로 Map 사용.
    //저장 구조: <세션 ID, 세션 ID에 해당하는 HttpSession>
    //예: sessions.put("42B9840241DCE8A58AB57822AA8A06AF", HttpSession 객체);
    //예: sessions.put("session.getId(), session)
    //key(String): 세션 ID -> session.getId()로 얻는다
    //value(HttpSession): 세션 자체 -> 사용자 정보 등 담겨있다
    private final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();


    /**
     * 현재 유효한 세션이 있는지 조회(찾는) 메서드
     * @param sessionId 로 Map(sessions)에 저장되어 있는지 조회
     */
    public HttpSession findById(String sessionId) {
        System.out.println("findById 메서드) sessionId: " + sessionId);

        //세션이 존재하면 session 객체를 리턴하고, 아니면 null 리턴
        return sessions.get(sessionId);
    }

    /**
     * 새로운 세션이 만들어질 때 호출
     */
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        //세션이 만들어졌다면, Map(sessions)에 등록
        System.out.println("[세션 생성] sessionId : " + se.getSession().getId() + "getSession(): " + se.getSession());

        //se.getSession().getId(): HttpSession 객체에서 고유한 세션 ID(String)을 반환한다.
        //se.getSession(): HttpSession 객체를 반환한다.
        sessions.put(se.getSession().getId(), se.getSession());

        log.info("Session created: {}", se.getSession().getId());
    }


    /**
     * 세션이 만료되거나 로그아웃으로 삭제될 때 호출
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        //세션이 사라졌을 때
        System.out.println("[세션 삭제] sessionId : " + se.getSession().getId() + "getSession(): " + se.getSession());
        sessions.remove(se.getSession().getId());

        log.info("Session destroyed: {}", se.getSession().getId());
    }
}
