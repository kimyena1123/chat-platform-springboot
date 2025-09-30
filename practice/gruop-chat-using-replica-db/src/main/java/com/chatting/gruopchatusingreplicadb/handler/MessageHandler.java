package com.chatting.gruopchatusingreplicadb.handler;

import com.chatting.gruopchatusingreplicadb.dto.Message;
import com.chatting.gruopchatusingreplicadb.entity.MessageEntity;
import com.chatting.gruopchatusingreplicadb.repository.MessageRepository;
import com.chatting.gruopchatusingreplicadb.session.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [MessageHandler]
 * - Spring WebSocket 서버에서 텍스트 메시지를 처리하는 핵심 핸들러.
 * - 연결/오류/종료 이벤트를 관리하고, 수신된 텍스트(JSON)를 파싱해 저장 및 브로드캐스트한다.
 *
 * 동작 개요(카카오톡 비유):
 *  1) 클라이언트(Web)가 소켓을 연결하면 세션을 등록한다(누가 접속했는지 기억).
 *  2) 사용자가 메시지를 보내면(JSON 페이로드):
 *     - JSON을 Message DTO로 파싱 → DB에 저장 → 보낸 사람을 제외한 다른 모든 참가자에게 전송.
 *  3) 연결 중 오류가 나거나 연결이 끊기면 세션을 정리한다(리소스 누수 방지).
 *
 * 주요 협력자:
 *  - WebSocketSessionManager: 현재 접속 중인 모든 세션을 보관/조회/해제하는 역할(세션 레지스트리).
 *  - MessageRepository: 수신한 채팅 메시지를 DB에 저장/조회하는 JPA 리포지토리.
 *  - ObjectMapper: 수신 텍스트(JSON) ↔ DTO(Message) 변환.
 */
@Slf4j
@Component
public class MessageHandler extends TextWebSocketHandler {

    // JSON 직렬화/역직렬화를 위한 Jackson 도구.
    // (실무에서는 @Bean 주입을 선호하지만, 여기서는 간단히 직접 생성)
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 현재 연결된 WebSocket 세션들을 관리(등록/해제/전체조회)하는 매니저
    private final WebSocketSessionManager webSocketSessionManager;

    // 메시지를 DB에 저장/조회하기 위한 JPA 리포지토리
    private final MessageRepository messageRepository;

    public MessageHandler(
            WebSocketSessionManager webSocketSessionManager, MessageRepository messageRepository) {
        this.webSocketSessionManager = webSocketSessionManager;
        this.messageRepository = messageRepository;
    }

    /**
     * 클라이언트가 WebSocket 연결에 성공했을 때 호출.
     * - 세션을 안전하게 감싸는 데코레이터(ConcurrentWebSocketSessionDecorator)로 래핑하여
     *   느린 클라이언트로 인한 서버 자원 고갈을 방지한다(전송 타임아웃/버퍼 제한).
     * - 래핑된 세션을 세션 매니저에 저장해, 이후 브로드캐스트 대상에 포함시킨다.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("ConnectionEstablished: {}", session.getId());

        /**
         * ConcurrentWebSocketSessionDecorator
         * - 전송 타임아웃(ms)과 전송 버퍼 최대 크기(bytes)를 제한하는 보호막.
         * - 5000ms: 이 시간 안에 전송이 안 되면 해당 전송을 실패 처리하여 서버가 오래 블로킹되지 않게 함.
         * - 100 * 1024: 100KB를 초과해서 전송 큐가 쌓이는 것을 막아, 느린 소비자(slow consumer)로 인한 메모리 폭증 방지.
         */
        ConcurrentWebSocketSessionDecorator safeSession =
                new ConcurrentWebSocketSessionDecorator(session, 5000, 100 * 1024);

        // 현재 접속한 세션을 레지스트리에 등록 → 이후 전체 전송 대상에 포함 가능
        webSocketSessionManager.storeSession(safeSession);
    }

    /**
     * 전송 과정에서 예기치 못한 I/O 오류 등이 발생했을 때 호출.
     * - 로그를 남기고, 문제가 있는 세션은 매니저를 통해 정리한다(리소스 누수 방지).
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());
        webSocketSessionManager.terminateSession(session.getId());
    }

    /**
     * 클라이언트가 정상/비정상적으로 연결을 끊었을 때 호출.
     * - 로그를 남기고 세션을 레지스트리에서 제거하여 더 이상 전송 대상에 포함되지 않게 한다.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        log.info("ConnectionClosed: [{}] from {}", status, session.getId());
        webSocketSessionManager.terminateSession(session.getId());
    }

    /**
     * 실제 텍스트 메시지를 수신했을 때 호출되는 핵심 메서드.
     * - 클라이언트가 보낸 문자열(payload)을 처리한다.
     *
     * 프로토콜:
     *  - 일반 메시지: JSON 형태여야 한다. 예) {"username":"alice","content":"hello"}
     *  - 특수 명령: "/last" 를 보내면 DB에서 가장 최근 메시지 1건을 조회해 보낸 사람에게만 회신.
     */
    @Override
    protected void handleTextMessage(WebSocketSession senderSession, @NonNull TextMessage message) {
        log.info("Received TextMessage: [{}] from {}", message, senderSession.getId());
        String payload = message.getPayload();

        // 1) 특수 명령 처리: "/last" → 최신 메시지 1건을 조회해 현재 보낸 클라이언트에게만 반환
        if (payload.equals("/last")) {
            // DB에서 가장 최근 메시지(메시지 시퀀스가 가장 큰 것)를 찾는다.
            messageRepository.findTopByOrderByMessageSequenceDesc()
                    .ifPresent(last -> sendMessage(
                            senderSession,
                            new Message(last.getUsername(), last.getContent()))
                    );
            return; // 특수 명령 처리 후 종료
        }

        // 2) 일반 메시지 처리: JSON 파싱 → 저장 → 브로드캐스트
        try {
            // (a) JSON 문자열 → Message DTO로 역직렬화 (username, content 필드가 있어야 함)
            Message receivedMessage = objectMapper.readValue(payload, Message.class);

            // (b) DB 저장: 기록(감사/복구용), 이후 검색/리포트 등에 활용 가능
            messageRepository.save(
                    new MessageEntity(receivedMessage.username(), receivedMessage.content()));

            // (c) 브로드캐스트: 현재 연결된 모든 참가자에게 전파(단, 보낸 사람은 제외)
            webSocketSessionManager
                    .getSessions()
                    .forEach(participantSession -> {
                        // 같은 세션(보낸 사람)에게는 다시 보내지 않음(에코 방지)
                        if (!senderSession.getId().equals(participantSession.getId())) {
                            sendMessage(participantSession, receivedMessage);
                        }
                    });

        } catch (Exception ex) {
            // JSON 파싱 실패 등 프로토콜 위반 시: 보낸 사람에게 에러 알림 전송
            String errorMessage = "Invalid protocol."; // 클라이언트가 형식을 지키지 않았음
            log.error("errorMessage payload: {} from {}", payload, senderSession.getId());
            sendMessage(senderSession, new Message("system", errorMessage));
        }
    }

    /**
     * 안전하게 텍스트 메시지를 1명에게 전송하는 유틸.
     * - DTO(Message) → JSON 문자열 → WebSocket 텍스트 메시지로 변환 후 전송
     * - 전송 실패 시 로그만 남긴다(특정 클라이언트의 일시적 오류가 전체 서비스에 영향 주지 않도록).
     */
    private void sendMessage(WebSocketSession session, Message message) {
        try {
            // 객체 → JSON 문자열 직렬화
            String msg = objectMapper.writeValueAsString(message);

            // 실제 전송
            session.sendMessage(new TextMessage(msg));

            log.info("Send message: {} to {}", msg, session.getId());
        } catch (Exception ex) {
            log.error("Failed to send message to {} error: {}", session.getId(), ex.getMessage());
        }
    }
}
