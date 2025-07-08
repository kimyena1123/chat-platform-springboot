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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * WebSocket 통신을 처리하는 핸들러 클래스
 * 1:1 채팅 구조를 기반으로 하고 있으며, 최대 두명의 유저만 동시 접속할 수 있도록 설계되어 있다.
 */
@Slf4j
@Component
public class MessageHandler extends TextWebSocketHandler {

    //JSON 객체 변환기: Java 객체 <-> JSON 문자열(Jackson 라이브러리 사용)
    //클라이언트와 JSON 문자열로 데이터를 주고받기 때문에 필요
    private final ObjectMapper objectMapper = new ObjectMapper();

    //1:1 채팅 구조에서 각각의 사용자 세션을 저장
    //동시에 최대 두명의 사용자만 접속 가능하므로 leftSide, rightSide 두 개만 사용
    private WebSocketSession leftSide = null;
    private WebSocketSession rightSide = null;

    //각 세션(WebSocketSession)마다 사용자의 이름을 저장하는 Map
    //세션을 통해 메시지를 받을 때 누가 보냈는지 알기 위해 필요
    private final Map<WebSocketSession, String> sessionNameMap = new HashMap<>();

    /**
     * 클라이언트가 WebSocket 연경를 맺었을 때 호출되는 메서드
     * 두 명까지만 연결 허용하며, 세번째 사용자는 거절된다
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        log.info("ConnectionEstablished: {}", session.getId());

        //요청 헤더에서 "name" 값을 가져와서 사용자 이름으로 사용
        String name = session.getHandshakeHeaders().getFirst("name");

        //세션과 사용자 이름 매핑 저장
        sessionNameMap.put(session, name);

        //세션등록: 첫번째 접속자라면 leftSide로 등록
        if(leftSide == null){ //session이 비어 있으면(session 등록이 안되어 있으면) 등록한다.
            leftSide = session; // 첫번째 유저 등록
            sendJoinMessage(name); //입장 메시지 브로드캐스트
            return;
        } else if(rightSide == null){//누군가 먼저 접속해서 session을 사용하고 있다면(등록을 완료했다면), 반대쪽 세션을 등록한다.
            rightSide = session; // 두번째 유저 등록
            sendJoinMessage(name);
            return;
        }

        //이미 두 명이 연결된 상태라면 접속 거부(양쪽 다 등록이 완료되어 있으면 1:1 채팅에서는 접속을 더이상 받을 수 X)
        log.warn("빈 자리 없음. {}의 접속 거부.", session.getId());

        //session을 닫는다. client는 접속할 수 없다.
        session.close();
    }

    /**
     * 연결 중 에러 발생 시 호출되는 메서드
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("TransportError: [{}] from {}", exception.getMessage(), session.getId());
    }

    /**
     * 클라이언트가 WebSocket 연결을 종료했을 때 호출되는 메서드
     * 종료된 세션을 제거해 다른 사용자의 재접속이 가능하도록 처리
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        //등록된 세션을 정리
        log.info("ConnectionClosed: [{}] from {}", status, session.getId());

        //사용자 이름 정보도 제거
        sessionNameMap.remove(session);

        //세션을 반납(세션은 자동으로 닫힐텐데, leftSide와 rightSide 세션을 등록한 걸 초기화 시켜서 다른 접속을 받을 수 있는 형태를 만들어줘야 한다
        if(leftSide == session){
            leftSide = null;
        }else if(rightSide == session){
            rightSide = null;
        }
    }

    /**
     * 클라이언트가 메시지를 전송하면 호출되는 메서드
     * 상대방에게 메시지를 전달하는 역할 수행
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received TextMessage: [{}] from {}", message, session.getId());

        String payload = message.getPayload(); // JSON 형식 문자열 수신. payload에 JSON이 담겨있다.

        try{
            //JSON 문자열을 Message 객체로 변환한다. 매개변수: (변환할 스트링 값(현재 읽어야할 JSON 값이 들어있는 것), 어떤 값을 만들어서 매핑할 건지)
            Message receivedMessage = objectMapper.readValue(payload, Message.class);

            //메시지를 보낸 사람이 왼쪽인지 오른쪽인지 판단하고, 상대방에게 메시지 전달
            if (leftSide == session) {//송신자가 leftSide이면 rightSide로 보낸다
                sendMessage(rightSide, receivedMessage.content(), session);
            }else if(rightSide == session){
                sendMessage(leftSide, receivedMessage.content(), session);
            }
        }catch (Exception ex){
            //JSON 변환 실패 시 에러 메시지 전송
            String errorMessage = "유효한 프로토콜이 아닙니다.";
            log.error("errorMessage payload: {} from {}", payload, session.getId());
            sendMessage(session, errorMessage, session);
        }

    }

    /**
     * 사용자가 입장했을 때 호출되는 메서드
     * 현재 접속한 모든 유저(최대 2명)에게 "OO님이 입장하셨습니다" 메시지를 전송
     */
    private void sendJoinMessage(String name) {
        String content = name + "님이 입장하셨습니다.";

        try {
            //Message 객체를 JSON 문자열로 변환
            String json = objectMapper.writeValueAsString(new Message(content, name));

            //두 세션이 열러 있으면 각각 메시지를 전송
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    if (leftSide != null && leftSide.isOpen()) {
                        leftSide.sendMessage(new TextMessage(json));
                    }
                    if (rightSide != null && rightSide.isOpen()) {
                        rightSide.sendMessage(new TextMessage(json));
                    }
                    log.info("입장 메시지 전송: {}", content);
                } catch (IOException e) {
                    log.error("입장 메시지 전송 실패: {}", e.getMessage());
                }
            });

            log.info("입장 메시지 전송: {}", content);
        } catch (IOException e) {
            log.error("입장 메시지 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 사용자 메시지를 특정 세션에게 전달하는 메서드
     * 메시지 송신자는 제외하고 수신자에게만 전송
     *
     * @param session 메시지를 받을 대상
     * @param message 내용 (content 필드 값)
     * @param senderSession 누가 보냈는지 (이름 추출용)
     */
    private void sendMessage(WebSocketSession session, String message, WebSocketSession senderSession) {
        try {
            //메시지 보낸 사람의 이름을 가져옴
            String senderName = sessionNameMap.getOrDefault(senderSession, "익명");

            //Message 객체를 JSON 문자열로 직렬화
            String json = objectMapper.writeValueAsString(new Message(message, senderName));

            //메시지를 전송
            session.sendMessage(new TextMessage(json));

            log.info("Send message: {} to {}", json, session.getId());
        } catch (Exception ex) {
            log.error("메시지 전송 실패 to {} error: {}", session.getId(), ex.getMessage());
        }
    }

}
