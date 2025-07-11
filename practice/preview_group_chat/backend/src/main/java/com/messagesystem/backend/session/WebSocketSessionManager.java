package com.messagesystem.backend.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {

    //session을 관리할 Map
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>(); //멀티쓰레드로 접근할 것이기에 ConcurrentHashMap 사용

    //전체 session 리스트를 리턴하는 메서드
    public List<WebSocketSession> getSessions(){

        return sessions.values().stream().toList(); //list로 변환
    }

    //session을 등록(저장)해줄 메서드
    public void storeSessions(WebSocketSession webSocketSession){

        log.info("Store Session : {}", webSocketSession.getId());

        //key: id, value: session으로 등록
        sessions.put(webSocketSession.getId(), webSocketSession);
    }

    //session을 삭제하는 메서드(기능을 삭제하고 session을 삭제하는 메서드)
    public void terminateSession(String sessionId){

        try {
            WebSocketSession webSocketSession = sessions.remove(sessionId);

            if(webSocketSession != null){ //null이 아니면 닫아준다
                //닫기 전에 닫을 세션이 무엇인지 로그로 남김
                log.info("Remove session : {}", sessionId);

                webSocketSession.close();

                //닫은 세션이 무엇인지 로그로 남김
                log.info("Close session: {}", sessionId);
            }
        }catch (Exception ex){
            log.error("Failed WebSocketSession close, sessionId: {}", sessionId);
        }
    }

}
