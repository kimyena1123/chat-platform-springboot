package com.chatting.gruopchatusingreplicadb.handler;

import com.chatting.gruopchatusingreplicadb.dto.Message;
import com.chatting.gruopchatusingreplicadb.repository.MessageRepository;
import com.chatting.gruopchatusingreplicadb.service.MessageService;
import com.chatting.gruopchatusingreplicadb.session.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [MessageHandler - 테스트 프로젝트용 WebSocket 텍스트 핸들러]
 *
 * 이 클래스는 Spring WebSocket 서버가 클라이언트와 통신할 때,
 * - 연결/해제/오류 이벤트를 받고
 * - 수신한 텍스트 메시지(payload)를 간단한 프로토콜에 따라 분기 처리한 뒤
 * 실제 동작(저장/조회/전송)은 MessageService로 위임합니다.
 *
 * 큰 흐름(카카오톡 비유)
 * 1) 클라이언트가 소켓을 연결하면, 이 세션을 중앙 레지스트리(WebSocketSessionManager)에 등록합니다.
 * → 이후 브로드캐스트할 때 “누가 접속 중인지”를 알고 보내줄 수 있음.
 * 2) 메시지를 받으면(JSON 텍스트):
 * → "/last" 명령이면 최신 1건을 조회해서 ‘보낸 사람에게만’ 회신.
 * → "/get N" 명령이면 N번 메시지를 조회해서 ‘보낸 사람에게만’ 회신.
 * → (일반 메시지라면) DB에 저장 후 ‘보낸 사람을 제외한 모두’에게 전송 (※ 아래 코드에선 기본 분기가 빠져 있어 주석으로 안내)
 * 3) 오류나 종료가 발생하면 세션을 정리(등록 해제)해 리소스 누수 방지.
 *
 * 협력자(주요 의존성)
 * - WebSocketSessionManager: 현재 접속 중인 세션들을 저장/조회/삭제(브로드캐스트 대상 관리)
 * - MessageService         : 메시지 저장/조회/전송(실제 비즈니스)을 담당
 * - ObjectMapper           : 텍스트(JSON) ↔ DTO(Message) 변환
 * - MessageRepository      : (여기선 직접 사용하지 않음) DB 접근용 JPA 리포지토리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();   // JSON 직렬화/역직렬화를 위한 Jackson 도구.
    private final WebSocketSessionManager webSocketSessionManager;  // 현재 연결된 WebSocket 세션들을 관리(등록/해제/전체조회)하는 매니저
    private final MessageService messageService;
    private final MessageRepository messageRepository;  // 메시지를 DB에 저장/조회하기 위한 JPA 리포지토리


    /**
     * [연결 성공 콜백]
     * 클라이언트가 WebSocket 연결을 맺었을 때 호출된다.
     *
     * 여기서 하는 일:
     * - 세션을 '보호용 데코레이터'로 감싸 타임아웃/버퍼 제한을 걸어 둠
     * (느린 클라이언트 때문에 서버 메모리가 무한정 쌓이는 것을 방지)
     * - 감싼 세션을 매니저에 등록 → 이후 브로드캐스트 대상에 포함
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("ConnectionEstablished: {}", session.getId());

        /*
         * ConcurrentWebSocketSessionDecorator
         *  - 내부 전송 큐(버퍼)와 전송 타임아웃을 제한하는 래퍼.
         *  - 두 인자 의미:
         *      1) 5000(ms) : 이 시간 안에 전송이 완료되지 않으면 해당 전송을 포기(서버 블로킹 방지)
         *      2) 100 * 1024(bytes = 100KB) : 세션별 전송 대기 버퍼의 상한(느린 소비자로 인한 메모리 폭증 방지)
         */
        ConcurrentWebSocketSessionDecorator safeSession =
                new ConcurrentWebSocketSessionDecorator(session, 5000, 100 * 1024);

        // 현재 세션을 중앙 레지스트리에 등록(이후 전체 전송 시 대상이 됨)
        webSocketSessionManager.storeSession(safeSession);
    }

    /**
     * [전송 중 오류 콜백]
     * 네트워크 I/O 등 예기치 못한 오류가 발생했을 때 호출됩니다.
     * - 로그를 남기고, 문제가 된 세션은 레지스트리에서 제거(리소스 누수/잘못된 참조 방지)
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());
        webSocketSessionManager.terminateSession(session.getId());
    }


    /**
     * [연결 종료 콜백]
     * 정상/비정상 종료를 막론하고, 세션이 닫힐 때 호출됩니다.
     * - 로그를 남기고 세션을 레지스트리에서 제거 → 더 이상 브로드캐스트 대상이 아님
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        log.info("ConnectionClosed: [{}] from {}", status, session.getId());
        webSocketSessionManager.terminateSession(session.getId());
    }


    /**
     * [텍스트 메시지 수신 콜백 - 핵심]
     * 클라이언트가 텍스트 메시지(문자열)를 보냈을 때 호출됩니다.
     *
     * 간단한 프로토콜:
     * - "/last"           : DB에서 가장 최근 메시지 1건을 찾아 '보낸 사람'에게만 회신
     * - "/get <number>"   : 지정 시퀀스 번호 메시지를 찾아 '보낸 사람'에게만 회신
     * - (그 외 일반 JSON) : 보통은 DB에 저장 후, '보낸 사람을 제외한 모두'에게 브로드캐스트
     * (※ 현재 이 기본 분기는 코드에 빠져 있음. 필요하면 마지막 else에 추가)
     */
    @Override
    protected void handleTextMessage(WebSocketSession senderSession, @NonNull TextMessage message) {
        log.info("Received TextMessage: [{}] from {}", message, senderSession.getId());
        String payload = message.getPayload();

        // (1) 특수 명령 1: "/last"
        //     - 가장 최근 메시지 1건을 Optional로 반환하고, 있으면 보낸 사람에게만 전송
        if (payload.equals("/last")) {
            messageService
                    .getLastMessage()
                    .ifPresent(msg -> messageService.sendMessage(senderSession, msg));
        }
        // (2) 특수 명령 2: "/get <number>"
        //     - 예: "/get 42" → 42번 메시지 조회
        else if (payload.contains("/get")) { // ex: /get {number}

            // 공백 기준으로 나눔
            // - split[0] : /get
            // - split[1] : 숫자
            String[] split = payload.split(" ");
            if (split.length > 1) {
                try {
                    // "/get 123" 의 123 부분을 숫자로 파싱
                    // 해당 번호의 메시지를 조회해서 있으면 보낸 사람에게만 회신
                    messageService.getMessage(Long.valueOf(split[1]))
                            .ifPresent(msg -> messageService.sendMessage(senderSession, msg));
                } catch (Exception ex) {
                    // 숫자로 파싱 실패 등 ‘프로토콜 위반’
                    String errorMessage = "Invalid protocol.";
                    log.error("Get request failed. cause: {}", ex.getMessage());
                    messageService.sendMessage(senderSession, new Message("system", errorMessage));
                }
            }
        } else {
            try {
                messageService.sendMessageToAll (senderSession, payload);
            } catch (Exception ex) {
                log.error("Failed to send message to {} error: {}", senderSession.getId(), ex.getMessage());
            }
        }
    }


}
