package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;

public class RejectResponse extends BaseMessage{

    private final String username;
    private final UserConnectionStatus status;  //최종상태

    public RejectResponse(String username, UserConnectionStatus status) {
        super(MessageType.REJECT_RESPONSE);

        this.username = username;
        this.status = status;
    }

    //getter
    public String getUsername() {
        return username;
    }

    public UserConnectionStatus getStatus() {
        return status;
    }
}
