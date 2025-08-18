package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;

public class InviteNotification extends BaseMessage{

    //client에게 전달해야 할 정보가 username이 있다.
    private final String username;

    public InviteNotification(String username) {
        super(MessageType.ASK_INVITE);

        this.username = username;
    }

    //getter
    public String getUsername() {
        return username;
    }
}
