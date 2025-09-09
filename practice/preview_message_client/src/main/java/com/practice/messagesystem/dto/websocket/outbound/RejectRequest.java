package com.practice.messagesystem.dto.websocket.outbound;

import com.practice.messagesystem.contants.MessageType;

//누군가 보낸 쵸대 요청을 겆러하고 싶을 때
public class RejectRequest extends BaseRequest {

    //초대 보낸 사람의 username
    //서버가 "누구의 요청을 거절했는지" 알아야 한다
    private final String username;

    public RejectRequest(String username) {
        super(MessageType.REJECT_REQUEST);
        this.username = username;
    }

    //Getter
    public String getUsername() {
        return username;
    }
}
