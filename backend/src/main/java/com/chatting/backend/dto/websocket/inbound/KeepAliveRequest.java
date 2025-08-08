package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constants.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;

public class KeepAliveRequest extends BaseRequest {

    @JsonCreator
    public KeepAliveRequest() {
        super(MessageType.KEEP_ALIVE);
    }
}