package com.messagesystem.preview_direct_chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messagesystem.preview_direct_chat.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 통신을 처리하는 핸들러 클래스
 * 1:1 채팅 구조를 기반으로 하고 있으며, 최대 두명의 유저만 동시 접속할 수 있도록 설계되어 있다.
 */
@Slf4j
@Component
public class MessageHandler extends TextWebSocketHandler {

    // JSON 변환 도구: Java 객체 <-> JSON 문자열 변환(JSON으로 데이터를 주고받을 것이기 때문에 ObjectMapper가 필요하다)
    private final ObjectMapper objectMapper = new ObjectMapper();

    //1:1 채팅을 위한 두 개의 세션 저장 변수
    //client는 상대 session을 몰라도 되지만 server는 양쪽을 다 알고 있어야 하기 때문에, 지금은 1:1채팅 상황을 구현하는 상황이니까 일단 단순하게 변수 2개를 가지고서 양쪽 세션을 관리하도록 할 것이다.
    private WebSocketSession leftSide = null;
    private WebSocketSession rightSide = null;

    //====== Session을 설정해주는 상황 =====

    /**
     * 클라이언트가 WebSocket 연경를 맺었을 때 호출되는 메서드
     * 두 명까지만 연결 허용하며, 세번째 사용자는 거절된다
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("ConnectionEstablished: {}", session.getId());

        //처음에 client가 접속하면 session 상태는 null인 상태일거고
        if(leftSide == null){ //session이 비어 있으면(session 등록이 안되어 있으면) 등록한다.
            leftSide = session; // 첫번째 유저 등록
            return;
        } else if(rightSide == null){//누군가 먼저 접속해서 session을 사용하고 있다면(등록을 완료했다면), 반대쪽 세션을 등록한다.
            rightSide = session; // 두번째 유저 등록
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
                sendMessage(rightSide, receivedMessage.content());
            }else if(rightSide == session){
                sendMessage(leftSide, receivedMessage.content());
            }
        }catch (Exception ex){
            //JSON 변환 실패 시 에러 메시지 전송
            String errorMessage = "유효한 프로토콜이 아닙니다.";
            log.error("errorMessage payload: {} from {}", payload, session.getId());
            sendMessage(session, errorMessage);
        }

    }

    /**
     * 주어진 세션에 텍스트 메시지를 JSON 형태로 전송하는 메서드
     * @param session 메시지를 보낼 대상
     * @param message 실제 전송할 메시지 내용
     */
    private void sendMessage(WebSocketSession session, String message) {
        try{
            //Message 객체를 JSON 문자열로 직렬홛
            String msg = objectMapper.writeValueAsString(new Message(message)); //json으로 주고 받을 거라서 objectMapper 사용
            session.sendMessage(new TextMessage(msg));;
            log.info("Send message: {} to {}", msg, session.getId());
        }catch (Exception ex){
            log.error("메시  지 전송 실패 to {} error: {}", session.getId(), ex.getMessage());
        }
    }
}
