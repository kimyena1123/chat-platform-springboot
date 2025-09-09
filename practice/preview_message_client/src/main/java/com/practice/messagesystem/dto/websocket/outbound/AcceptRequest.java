package com.practice.messagesystem.dto.websocket.outbound;

import com.practice.messagesystem.contants.MessageType;

//상황: 누군가 나를 초대했을 때, 내가 그 요청을 수락하는 경우
public class AcceptRequest extends BaseRequest{

    //초대한 사람의 username
    //서버가 "누구의 요청을 수락하는지" 알아야 하기 때문
    private final String username;

    public AcceptRequest(String type) {
        super(MessageType.ACCEPT_REQUEST);

        this.username = type;
    }

    public String getUsername() {
        return username;
    }
}
