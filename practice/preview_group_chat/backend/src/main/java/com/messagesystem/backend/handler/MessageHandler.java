package com.messagesystem.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messagesystem.backend.dto.Message;
import com.messagesystem.backend.session.WebSocketSessionManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor //생성자 주입과 같은 역할
public class MessageHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketSessionManager webSocketSessionManager;


    /**
     * 클라이언트가 WebSocket 연경를 맺었을 때 호출되는 메서드
     * 두 명까지만 연결 허용하며, 세번째 사용자는 거절된다
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("ConnectionEstablished: {}", session.getId());

        //Session 등록
        webSocketSessionManager.storeSessions(session);
    }

    /**
     * 연결 중 에러 발생 시 호출되는 메서드
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());

        //문제가 된 session을 삭제
        webSocketSessionManager.terminateSession(session.getId());
    }

    /**
     * 클라이언트가 WebSocket 연결을 종료했을 때 호출되는 메서드
     * 종료된 세션을 제거해 다른 사용자의 재접속이 가능하도록 처리
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        log.info("ConnectionClosed: [{}] from {}", status, session.getId());

        webSocketSessionManager.terminateSession(session.getId());
    }

    /**
     * 클라이언트가 메시지를 전송하면 호출되는 메서드
     * 상대방에게 메시지를 전달하는 역할 수행
     */
    @Override
    protected void handleTextMessage(WebSocketSession senderSession, @NonNull TextMessage message) {
        log.info("Received TextMessage: [{}] from {}", message, senderSession.getId());
        String payload = message.getPayload();
        try {
            Message receivedMessage = objectMapper.readValue(payload, Message.class);

            //전체 session 다 받은 뒤에, 그룹채팅의 참여자
            webSocketSessionManager.getSessions().forEach(participantSession -> {
                //getSessions()으로 가져온 참여자 중에는 메시지를 보낸 송신자도 포함되어 있다
                //송신자와 getSession()으로 가져온 참여자와 같지 않으면(송신자 제외 나머지 참여자들을 의미함), 메시지를 발송함
                if(!senderSession.getId().equals(participantSession.getId())) {
                    sendMessage(participantSession, receivedMessage);
                }
            });

        } catch (Exception ex) {
            String errorMessage = "유효한 프로토콜이 아닙니다.";
            log.error("errorMessage payload: {} from {}", payload, senderSession.getId());

            sendMessage(senderSession, new Message("system", errorMessage));
        }
    }

    /**
     * 주어진 세션에 텍스트 메시지를 JSON 형태로 전송하는 메서드
     * @param session 메시지를 보낼 대상
     * @param message 실제 전송할 메시지 내용
     */
    private void sendMessage(WebSocketSession session, Message message) {
        try {
            String msg = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(msg));

            log.info("send message: {} to {}", msg, session.getId());
        } catch (Exception ex) {
            log.error("메시지 전송 실패 to {} error: {}", session.getId(), ex.getMessage());
        }
    }
}
