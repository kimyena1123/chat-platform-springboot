package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.InviteCode;
//수락한 본인에게 "ACCEPT_RESPONSE"
public class AcceptResponse extends BaseMessage{

    //누가 나의 요청을 수락했다
    private final String username;

    public AcceptResponse(String username) {
        super(MessageType.ACCEPT_RESPONSE);

        this.username = username;
    }

    //getter
    public String getUsername() {
        return username;
    }
}
