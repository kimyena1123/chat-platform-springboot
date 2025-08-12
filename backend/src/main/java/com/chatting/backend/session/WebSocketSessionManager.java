package com.chatting.backend.session;

import com.chatting.backend.dto.domain.Message;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {


    //session을 관리할 Map
    ///key: id, value: session으로 등록
    //원래는 세션을 관리하는 key 값으로 sessionId로 했는데 이건 unique한 값을 찾기 위해서 임시로 sessionId로 key 값을 지정한 것이고ㅡ
    //이제는 userId가 생겨서 userId로 key 값을 변경!
    private final Map<UserId, WebSocketSession> sessions = new ConcurrentHashMap<>(); //멀티스레드로 접근할 것이기에 ConcurrentHashMap 사용

    private final JsonUtil jsonUtil;

    /**
     * 전체 session 리스트를 리턴하는 메서드
     */
    public List<WebSocketSession> getSessions(){
        return sessions.values().stream().toList(); //List로 변환
    }

    /**
     * userId로 하나의 session을 리턴하는 메서드
     */
    public WebSocketSession getSession(UserId userId) {
        return sessions.get(userId);
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

    /**
     * 각 세션을 메시지를 보내는 메서드
     */
    /**
     * 주어진 세션에 텍스트 메시지를 JSON 형태로 전송하는 메서드
     * @param session 메시지를 보낼 대상(채팅 받는 사람)
     * @param message 실제 전송할 메시지 내용(보낸 사람의 username과 메시지 content가 담겨있다)
     */
    public void sendMessage(WebSocketSession session, Message message) {

        jsonUtil.toJson(message).ifPresent(msg -> {
           try{
               session.sendMessage(new TextMessage(msg));

               log.info("send message: {} to {}", msg, session.getId());
           }catch(Exception ex){
               log.error("메시지 전송 실패. cause: {}", ex.getMessage());
           }
        });

    }
}
