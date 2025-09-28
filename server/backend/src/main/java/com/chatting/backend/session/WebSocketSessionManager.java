package com.chatting.backend.session;

import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 연결(Session)을 관리하는 클래스
 * - 사용자별 WebSocketSession 저장
 * - 세션 닫기
 * - 메시지 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {


    // 사용자별 WebSocket 세션을 관라힐 Map (멀티스레드 환경이라 ConcurrentHashMap 사용)
    //  - key: id, value: session으로 등록
    private final Map<UserId, WebSocketSession> sessions = new ConcurrentHashMap<>(); //멀티스레드로 접근할 것이기에 ConcurrentHashMap 사용

    private final JsonUtil jsonUtil;

    /**
     * userId로 하나의 session을 리턴하는 메서드
     */
    public WebSocketSession getSession(UserId userId) {
        return sessions.get(userId);
    }

    /**
     * 전체 session 리스트를 리턴하는 메서드
     */
    public List<WebSocketSession> getSessions(){
        return sessions.values().stream().toList(); //List로 변환
    }

    /**
     * session을 등록(저장)하는 메서드
     */
    public void putSessions(UserId userId, WebSocketSession webSocketSession) {
        log.info("Store Session {}", webSocketSession.getId());

        //key: userId, value: session으로 등록
        sessions.put(userId, webSocketSession);
    }

    /**
     * session을 삭제하는 메서드
     */
    public void closeSession(UserId userId) {
        try{
            WebSocketSession webSocketSession = sessions.remove(userId);

            if(webSocketSession != null) { //null이 아니면 닫아준다.
                //닫기 전에 닫을 세션이 무엇인지 로그로 남김
                log.info("Remove session: {}", userId);

                webSocketSession.close();

                //닫은 세션이 무엇인지 로그로 남김
                log.info("Close session: {}", userId);
            }
        }catch (Exception ex){
            log.error("Failed WebSocketSession close, userId: {}", userId);
        }
    }


    // 문자열(JSON)을 그대로 WebSocket으로 전송
    public void sendMessage(WebSocketSession session, String message) throws IOException{
        try{
            session.sendMessage(new TextMessage(message));
            log.info("Send message: {} to {}", message, session.getId());
        }catch (IOException ex){
            log.error("Send message failed. cause: {}", ex.getMessage());
            throw ex;
        }
    }
}
