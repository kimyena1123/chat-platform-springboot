package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;

public class DisconnectResponse extends BaseMessage{

    private final String username;
    private final UserConnectionStatus status;

    public DisconnectResponse(String username, UserConnectionStatus status) {
        super(MessageType.DISCONNECT_RESPONSE);

        this.username = username;
        this.status = status;
    }

    //Getter
    public String getUsername() {
        return username;
    }

    public UserConnectionStatus getStatus() {
        return status;
    }
}
