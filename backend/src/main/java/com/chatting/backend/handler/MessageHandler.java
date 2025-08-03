package com.chatting.backend.handler;

import com.chatting.backend.dto.Message;
import com.chatting.backend.session.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 연결을 처리하는 핵심 핸들러 클래스
 * 이 클래스는 사용자의 접속/종료/메시지 송수신 이벤트를 처리한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager webSocketSessionManager;

    /**
     * 클라이언트가 WebSocket 연결을 맺었을 때 호출되는 메서드
     * 두명까지만 연결 허용하며, 세번째 사용자는 거절된다
     * websocket 연결 맺고 > 세션 저장
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        log.info("ConnectionEstablished: {}", session.getId());

        // 성능 향상을 위해 세션을 데코레이터로 감싸서 전송량 및 버퍼제한 설정 (최대 100KB, 5초 제한)
        ConcurrentWebSocketSessionDecorator concurrentWebSocketSessionDecorator = new ConcurrentWebSocketSessionDecorator(session, 5000, 100 * 1024);

        //session 등록
        webSocketSessionManager.storeSessions(concurrentWebSocketSessionDecorator);
    }

    /**
     * 연결 중 에러 발생 시 호출되는 메서드
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());

        //문제가 된 세션을 삭제
        webSocketSessionManager.terminateSession(session.getId());
    }

    /**
     * 클라이언트가 WebSocket 연결을 종료했을 때 호출되는 메서드
     * 종료된 세션을 제거해 다른 사용자의 재접속이 가능하도록처리
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        log.info("ConnectionClosed: [{}] from {}", status, session.getId());

        webSocketSessionManager.terminateSession(session.getId());
    }

    /**
     * 클라이언트가 메세지를 전송하면 호출되는 메서드
     * 상대방에게 메시지를 전달하는 역할 수행
     */
    @Override
    protected void handleTextMessage(WebSocketSession senderSession, @NonNull TextMessage message) {
        log.info("Received TextMessage: [{}] from {}", message, senderSession.getId());

        String payload = message.getPayload();

        try{
            Message receivedMessage = objectMapper.readValue(payload, Message.class);

            //전체 세션을 List로 받아서(getSession()) 전체 참여자에게 메시지를 다 보내기
            webSocketSessionManager.getSessions().forEach(participantSession -> {
                //getSessions()으로 가져온 세션 리스트(모든 참여자) 중에는 메시지를 보낸 송신자도 포함되어 있다.
                //송신자와 getSession()으로 가져온 참여자와 같지 않으면(수신자; 송신자 제외 나머지 참여자들을 의미함), 메시자를 발송함
                if (!senderSession.getId().equals(participantSession.getId())) {
                    sendMessage(participantSession, receivedMessage); //보낼 대상, 보낼 메시지
                }
            });
        }catch (Exception ex){
            String errorMessage = "유효한 프로토콜이 아닙니다.";

            log.error("errormessage payload: {} from {}", payload, senderSession.getId());

            sendMessage(senderSession, new Message("system", errorMessage)); //메시지를 잘못 보낼 시 "system: 유효한 프로토콜이 아닙니다" 라고 뜬다.
        }
    }

    /**
     * 해당 세션(참여자; 수신자)에게 텍스트 메시지를 JSON 형태로 전송하는 메서드
     * @param session 메세지를 받을 수신자를 의미(메시지를 보낼 대상)
     * @param message 보낼 메시지를 의미(실제 전송할 메시지 내용)
     */
    public void sendMessage(WebSocketSession session, Message message) {
        try{
            String msg = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(msg));

            log.info("send message: {} to {}", msg, session.getId());
        }catch (Exception ex){
            log.error("메세지 전송 실패 to {} error: {}", session.getId(), ex.getMessage());
        }
    }


}
