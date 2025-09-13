package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;

public class KeepAlive extends BaseRequest {

    @JsonCreator
    public KeepAlive() {
        super(MessageType.KEEP_ALIVE);
    }
}