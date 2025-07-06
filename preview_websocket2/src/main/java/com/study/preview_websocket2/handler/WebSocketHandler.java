package com.study.preview_websocket2.handler;

import com.google.gson.Gson;
import com.study.preview_websocket2.ChatMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    //WebSocketSession을 리스트로 가지고 있는 필드
    //여기에 현재 연결 중인 클라이언트들이 존재한다고 보면 된다
    //누군가가 접속을 한다면 그 사람의 WebSocketSession을 리스트에 저장하고 접속을 끊는다면 그 사람의 WebSocketSession을 리스트에서 제거한다.
    private List<WebSocketSession> sessionList = new ArrayList<>();

    //연결을 맺고나서 실행되는 메소드
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        //WebSocketSession의 getHandShakeHeaders 메소드를 실행하면 헤더를 가져올 수 있다
        //그 헤더 중 key가 name인 value를 가져온다. 이는 곧 연결 중인 사용자의 이름을 의미한다
        String name = session.getHandshakeHeaders().get("name").get(0);

        //sessionList에 해당 유저의 WebSocketSession을 추가한다
        //이는 이 유저가 채팅에 참여하고 있다는 것을 의미하고 이 채팅에 참여 중인 다른 유저의 메시지를 받을 수 있다
        //이렇게 sessionList에 추가하고나면 sessionList에 있는 모든 WebSocketSession에 메시지를 보낸다
        sessionList.add(session);
        System.out.println("sessionList = " + sessionList.size());

        sessionList.forEach(s ->  {
            try {
                s.sendMessage(new TextMessage(name+"님께서 입장하셨습니다."));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    //메시지를 다루는 메소드
    //메시지를 보낼 때 누가 보냈는지와 보낸 시간을 함께 출력해야한다
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        Gson gson = new Gson();
        String name = session.getHandshakeHeaders().get("name").get(0);

        sessionList.forEach(s-> {
            try {
                ChatMessage chatMessage = gson.fromJson(message.getPayload(), ChatMessage.class);
                s.sendMessage(new TextMessage(name + " : "+chatMessage.getContent()+"["+chatMessage.getTime()+"]"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    //채팅에 참여 중인 모든 유저에게 퇴장 메시지를 보낸다
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessionList.remove(session);
        System.out.println("session = " + sessionList.size());

        String name = session.getHandshakeHeaders().get("name").get(0);

        sessionList.forEach(s -> {
            try{
                s.sendMessage(new TextMessage(name + "님께서 퇴장하셨습니다."));
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        });
    }

}
