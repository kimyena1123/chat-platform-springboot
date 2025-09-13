package com.chatting.backend.handler;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.BaseRequest;
import com.chatting.backend.handler.websocket.RequestDispatcher;
import com.chatting.backend.json.JsonUtil;
import com.chatting.backend.session.WebSocketSessionManager;
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
public class WebSocketHandler extends TextWebSocketHandler {

    private final JsonUtil jsonUtil;
    private final WebSocketSessionManager webSocketSessionManager;
    private final RequestDispatcher requestDispatcher;

    /**
     * 클라이언트가 WebSocket 연결을 맺었을 때 호출되는 메서드
     * websocket 연결 맺고 > 세션 저장
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        log.info("ConnectionEstablished: {}", session.getId());

        // 성능 향상을 위해 세션을 데코레이터로 감싸서 전송량 및 버퍼제한 설정 (최대 100KB, 5초 제한)
        ConcurrentWebSocketSessionDecorator concurrentWebSocketSessionDecorator = new ConcurrentWebSocketSessionDecorator(session, 5000, 100 * 1024);

        //현재 session의 userId 가져오기
        UserId userId = (UserId)session.getAttributes().get(IdKey.USER_ID.getValue());

        //session 등록
        webSocketSessionManager.putSessions(userId, concurrentWebSocketSessionDecorator);
    }

    /**
     * 연결 중 에러 발생 시 호출되는 메서드
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());

        //현재 세션의 userId를 가져오기
        UserId userId = (UserId) session.getAttributes().get(IdKey.USER_ID.getValue());

        //문제가 된 세션을 삭제
        webSocketSessionManager.closeSession(userId);
    }

    /**
     * 클라이언트가 WebSocket 연결을 종료했을 때 호출되는 메서드
     * 종료된 세션을 제거해 다른 사용자의 재접속이 가능하도록 처리
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        log.info("ConnectionClosed: [{}] from {}", status, session.getId());

        //현재 세션의 userId를 가져오기
        UserId userId = (UserId) session.getAttributes().get(IdKey.USER_ID.getValue());

        webSocketSessionManager.closeSession(userId);
    }

    /**
     * 그러면 WebSocketHandler
     * 이 서버로 들어오는 모든 WebSocket 요청은 여기로 들어오게 될테고 handleTextMessage() 이 메서드가 받아서
     * dispatchRequest()를 호출해서 여기서 각 handler로 각각 호출되는 거다.
     */
    @Override
    protected void handleTextMessage(WebSocketSession senderSession, @NonNull TextMessage message) {
        String payload = message.getPayload();
        log.info("Received TextMessage: [{}] from {}", payload, senderSession.getId());

        jsonUtil
                .fromJson(payload, BaseRequest.class)
                .ifPresent(msg -> requestDispatcher.dispatchRequest(senderSession, msg));
    }


}