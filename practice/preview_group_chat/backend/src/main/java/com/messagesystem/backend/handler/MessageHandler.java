package com.messagesystem.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messagesystem.backend.contants.Constants;
import com.messagesystem.backend.dto.domain.Message;
import com.messagesystem.backend.dto.websocket.inbound.BaseRequest;
import com.messagesystem.backend.dto.websocket.inbound.KeepAliveRequest;
import com.messagesystem.backend.dto.websocket.inbound.MessageRequest;
import com.messagesystem.backend.entity.MessageEntity;
import com.messagesystem.backend.repository.MessageRepository;
import com.messagesystem.backend.service.SessionService;
import com.messagesystem.backend.session.WebSocketSessionManager;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionService sessionService;
    private final WebSocketSessionManager webSocketSessionManager;
    private final MessageRepository messageRepository;


    /**
     * 클라이언트가 WebSocket 연경를 맺었을 때 호출되는 메서드
     * 두 명까지만 연결 허용하며, 세번째 사용자는 거절된다
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("ConnectionEstablished: {}", session.getId());

        // 성능 향상을 위해 세션을 데코레이터로 감싸서 전송량 및 버퍼제한 설정 (최대 100KB, 5초 제한)
        ConcurrentWebSocketSessionDecorator concurrentWebSocketSessionDecorator
                = new ConcurrentWebSocketSessionDecorator(session, 5000, 100 * 1024);

        //Session 등록
        webSocketSessionManager.storeSessions(concurrentWebSocketSessionDecorator);
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
        String payload = message.getPayload();
        log.info("Received TextMessage: [{}] from {}", payload, senderSession.getId());

        try {
            BaseRequest baseRequest = objectMapper.readValue(payload, BaseRequest.class);

            if (baseRequest instanceof MessageRequest messageRequest) {
                Message receivedMessage =
                        new Message(messageRequest.getUsername(), messageRequest.getContent());
                messageRepository.save(
                        new MessageEntity(receivedMessage.username(), receivedMessage.content()));
                webSocketSessionManager
                        .getSessions()
                        .forEach(
                                participantSession -> {
                                    if (!senderSession.getId().equals(participantSession.getId())) {
                                        sendMessage(participantSession, receivedMessage);
                                    }
                                });
            } else if (baseRequest instanceof KeepAliveRequest) {
                sessionService.refreshTTL(
                        (String) senderSession.getAttributes().get(Constants.HTTP_SESSION_ID.getValue()));
            }
        } catch (Exception ex) {
            String errorMessage = "Invalid protocol.";
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
