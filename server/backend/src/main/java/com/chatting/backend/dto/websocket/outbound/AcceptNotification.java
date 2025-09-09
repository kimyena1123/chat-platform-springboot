package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;

//초대한 상대에게 "NOTIFY_ACCEPT" 를 전송.
public class AcceptNotification extends BaseMessage{

    //누가 나의 요청을 수락했다
    private final String username;

    public AcceptNotification(String username) {
        super(MessageType.NOTIFY_ACCEPT);

        this.username = username;
    }

    //getter
    public String getUsername() {
        return username;
    }
}
