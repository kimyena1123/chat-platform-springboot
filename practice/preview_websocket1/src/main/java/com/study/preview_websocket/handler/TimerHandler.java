package com.study.preview_websocket.handler;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimerHandler extends TextWebSocketHandler { // WebSocket 요청을 처리하는 실제 핸들러 클래스

    // 한 개의 스레드로 구성된 스케줄링 서비스 생성(타이머 기능 위해 사용)
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    // 클라이언트가 WebSocket 연결을 성공했을 때 호출되는 메소드
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("ConnectionEstablishedL: {}", session.getId());
    }

    // 클라이언트와 서버 간 통신 에러 발생 시 호출
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());
    }

    // 클라이언트와의 연결이 종료되었을 때 호출됨
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        log.info("ConnectionClosed: [{}] from {}", status, session.getId());
    }

    // 클라이언트가 텍스트 메시지를 보내면 이 메소드가 실행됨
    @Override
    protected void handleTextMessage(WebSocketSession session, @NonNull TextMessage message) throws Exception {
        log.info("Received TextMessasge: [{}] from {}", message, session.getId());

        try {
            // 메시지 내용(payload)을 정수로 파싱
            long seconds = Long.parseLong(message.getPayload());

            // 요청된 시간 (timestamp)을 현재 시각으로 저장(타이머 식별용)
            Long timestamp = Instant.now().toEpochMilli();

            // 일정 시간이 지난 후 클라이언트에게 타이머 완료 메시지를 보냄
            scheduledExecutorService.schedule(() ->
                            sendMessage(session, String.format("%d에 등록한 %d초 타이머 완료.", timestamp, seconds))
                    , seconds, TimeUnit.SECONDS);

            // 타이머 등록 메시지를 즉시 전송
            sendMessage(session, String.format("%d에 등록한 %d초 타이머 등록 완료. ", timestamp, seconds));

        } catch (Exception ex) {
            // 숫자가 아닌 잘못된 메시지를 보낸 경우 예외 처리
            sendMessage(session, "정수가 아님. 타이머 등록 실패");
        }
    }

    // 클라이언트에게 텍스트 메시지를 전송하는 메소드
    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
            log.info("send message: {} to {}" , message, session.getId());
        } catch (Exception ex) {
            log.error("메시지 전송이 실패 to{} error: {}", session.getId(), ex.getMessage());
        }
    }
}
