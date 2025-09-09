package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;

public class AcceptRequest extends BaseRequest {

    //초대한 사람의 username
    //서버가 "누구의 요청을 수락하는지" 알아야 하기 때문
    private final String username;

    public AcceptRequest(String username) {
        super(MessageType.ACCEPT_REQUEST);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
