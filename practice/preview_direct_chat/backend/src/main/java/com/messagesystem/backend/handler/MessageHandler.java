package com.messagesystem.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messagesystem.backend.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 1:1 채팅을 처리하는 서버측 핸들러.
 *
 * 최대 두 개의 WebSocket 세션을 관리하고,
 * 각 세션의 사용자 이름을 저장하며 메시지 송수신과 연결 상태 변경을 처리한다.
 */
@Slf4j
@Component
public class MessageHandler extends TextWebSocketHandler {

    //JSON 객체 변환기: Java 객체 <-> JSON 문자열(Jackson 라이브러리 사용)
    //클라이언트와 JSON 문자열로 데이터를 주고받기 때문에 필요
    private final ObjectMapper objectMapper = new ObjectMapper();

    //1:1 채팅을 위해 두 명의 사용자 세션을 저장하는 변수이다.
    //leftSide: 첫번째 사용자 세션
    //rightSide: 두번째 사용자 세션
    private WebSocketSession leftSide = null;
    private WebSocketSession rightSide = null;

    //각 WebSocketSession(사용자 연결)별로 사용자 이름을 저장하는 Map
    //사용자가 메시지를 보낼 때, 누가 보냈는지 알기 위해 이름을 관리한다.
    private final Map<WebSocketSession, String> sessionNameMap = new HashMap<>();

    /**
     * 사용자가 WebSocket으로 서버에 연결을 성공했을 때, 호출되는 메서드
     *
     * 연결 요청시 URL 쿼리 마라미터로 사용자 이름(name)을 받아서 자장한다
     * 최대 두명까지만 접속을 허용한다 > 이미 두명 모두 접속 중이라면 세번째 연결을 거부한다
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        log.info("ConnectionEstablished: {}", session.getId());

        //1) 쿼리 마라미터에서 "name=값"을 직접 추출
        //URI 객체에서 쿼리 문자열을 가져와서 "name="파라미터를 찾아 디코딩
        String name = null;
        URI uri = session.getUri();
        if (uri != null && uri.getQuery() != null) {
            // 쿼리 파라미터가 여러 개일 수 있으니 '&'로 분리 후 하나씩 확인
            for (String param : uri.getQuery().split("&")) {
                if (param.startsWith("name=")) {
                    // name= 이후 부분을 UTF-8로 URL 디코딩
                    name = URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8);
                }
            }
        }

        log.info("➡️ 추출된 사용자 이름: {}", name);  // 확인 로그

        // 사용자 세션과 이름을 Map에 저장 (메시지 보낼 때 이름 확인용)
        sessionNameMap.put(session, name); // 이름 등록

        // 2) 빈 자리가 있으면 연결 수락 후 입장 메시지 전송
        if (leftSide == null) {
            leftSide = session;     // 왼쪽 자리 비어있으면 여기에 저장
            sendJoinMessage(name);  // 입장 알림 메시지 보내기
            return; // 메서드 종료
        } else if (rightSide == null) {
            rightSide = session;    // 오른쪽 자리 비어있으면 여기에 저장
            sendJoinMessage(name);  // 입장 알림 메시지 보내기
            return; // 메서드 종료
        }

        // 3) 두 자리가 모두 차있으면 접속 거부하고 세션 종료
        log.warn("빈 자리 없음. {}의 접속 거부.", session.getId());
        session.close();
    }

    /**
     * WebSocket 연결 중 에러가 발생하면 호출되는 메서드입니다.
     *
     * @param session 에러가 발생한 WebSocket 세션
     * @param exception 발생한 에러 예외 객체
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());
    }

    /**
     * 클라이언트가 WebSocket 연결을 종료했을 때 호출되는 메서드입니다.
     *
     * 종료된 세션을 내부에서 관리하는 변수에서 제거하여
     * 새로운 사용자가 접속할 수 있도록 공간을 확보합니다.
     *
     * @param session 종료된 WebSocket 세션
     * @param status 종료 상태 정보
     * @throws Exception 예외 발생 시 호출자에게 전달
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        //등록된 세션을 정리
        log.info("ConnectionClosed: [{}] from {}", status, session.getId());

        // 1) 해당 세션의 사용자 이름 정보도 Map에서 삭제
        sessionNameMap.remove(session);

        // 2) leftSide 또는 rightSide 중 종료된 세션을 null로 초기화해서
        // 다른 사용자가 접속할 수 있도록 자리를 비워둔다
        if(leftSide == session){
            leftSide = null;
        }else if(rightSide == session){
            rightSide = null;
        }
    }

    /**
     * 클라이언트가 서버에 텍스트 메시지를 보내면 호출되는 메서드입니다.
     *
     * 전달받은 메시지(JSON 형태)를 Message 객체로 변환하고,
     * 송신자의 상대방 세션으로 메시지를 전달합니다.
     *
     * @param session 메시지를 보낸 WebSocket 세션(송신자)
     * @param message 서버가 받은 텍스트 메시지 (JSON 문자열 포함)
     * @throws Exception 예외 발생 시 호출자에게 전달
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received TextMessage: [{}] from {}", message, session.getId());

        // 메시지의 실제 텍스트(payload)를 가져옴 (JSON 문자열임)
        String payload = message.getPayload();

        try{
            // 1) JSON 문자열을 Message 클래스 객체로 변환
            // Message 클래스는 content, name 필드를 가진 DTO 객체
            Message receivedMessage = objectMapper.readValue(payload, Message.class);

            // 2) 메시지를 보낸 세션이 leftSide인지 rightSide인지 확인 후,
            // 상대방 세션에 메시지를 전달함
            if (leftSide == session) {
                // 송신자가 leftSide일 때는 rightSide로 메시지 전송
                sendMessage(rightSide, receivedMessage.content(), session);
            }else if(rightSide == session){
                // 송신자가 rightSide일 때는 leftSide로 메시지 전송
                sendMessage(leftSide, receivedMessage.content(), session);
            }
        }catch (Exception ex){
            // 3) JSON 파싱 실패 등 예외 발생 시,
            // 유효하지 않은 프로토콜 메시지 알림을 송신자에게 전송
            String errorMessage = "유효한 프로토콜이 아닙니다.";
            log.error("errorMessage payload: {} from {}", payload, session.getId());

            // 오류 메시지 전송 (송신자에게 보냄)
            sendMessage(session, errorMessage, session);
        }

    }

    /**
     * 사용자가 채팅방에 입장했을 때, 현재 접속해 있는 모든 사용자에게 입장 메시지를 보내는 메서드
     *
     * @param name 입장한 사용자의 이름
     */
    private void sendJoinMessage(String name) {
        // 입장 메시지 내용 생성
        String content = name + "님이 입장하셨습니다.";

        try {
            // 새로운 쓰레드에서 비동기 처리하여 입장 메시지 전송
            // (웹소켓 sendMessage는 I/O 작업이므로 별도 쓰레드에서 하는 것이 안정적)
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    // leftSide 세션이 존재하고 연결이 열려있으면 메시지 전송
                    if (leftSide != null && leftSide.isOpen()) {
                        leftSide.sendMessage(new TextMessage(content));
                    }
                    // rightSide 세션도 동일하게 처리
                    if (rightSide != null && rightSide.isOpen()) {
                        rightSide.sendMessage(new TextMessage(content));
                    }
                    log.info("입장 메시지 전송: {}", content);
                } catch (IOException e) {
                    log.error("입장 메시지 전송 실패: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("입장 메시지 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 특정 WebSocket 세션에게 메시지를 전송하는 메서드
     *
     * @param session 메시지를 받을 대상 세션 (상대방)
     * @param message 메시지 내용 (텍스트)
     * @param senderSession 메시지를 보낸 송신자 세션 (자기 자신에게도 메시지 전송용)
     */
    private void sendMessage(WebSocketSession session, String message, WebSocketSession senderSession) {
        try {
            // 1) 송신자 세션을 기준으로 사용자 이름 조회 (없으면 "익명" 사용)
            String senderName = sessionNameMap.getOrDefault(senderSession, "익명");

            // 2) 메시지와 사용자 이름을 포함하는 Message 객체를 JSON 문자열로 변환
            String json = objectMapper.writeValueAsString(new Message(message, senderName));

            // 3) 상대방 세션이 null 아니고 연결이 열려있으면 메시지 전송
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }

            // 4) 송신자 자신에게도 동일한 메시지를 전송 (내가 보낸 메시지 화면에 표시 용도)
            if (senderSession != null && senderSession.isOpen()) {
                senderSession.sendMessage(new TextMessage(json));
            }

            log.info("Send message: {} to {}", json, session.getId());
        } catch (Exception ex) {
            log.error("메시지 전송 실패 to {} error: {}", session.getId(), ex.getMessage());
        }
    }

}
