package com.practice.messagesystem.dto.websocket.outbound;

import com.practice.messagesystem.contants.MessageType;

/** [사용자가 채팅 연결을 "끊으려" 할 때 서버로 보내는 요청 DTO] */
public class DisconnectRequest extends BaseRequest {

    //자기 자신(요청자)의 username
    //"내가 연결 끊을래"라는 의사 표시
    private final String username;

    public DisconnectRequest(String username) {
        super(MessageType.DISCONNECT_REQUEST);
        this.username = username;
    }

    //Getter
    public String getUsername() {
        return username;
    }
}