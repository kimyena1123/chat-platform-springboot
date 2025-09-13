package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DisconnectRequest extends BaseRequest{

    private final String username;

    @JsonCreator
    public DisconnectRequest(@JsonProperty("username") String username) {
        super(MessageType.DISCONNECT_REQUEST);

        this.username = username;
    }

    //GETTER
    public String getUsername() {
        return username;
    }
}
