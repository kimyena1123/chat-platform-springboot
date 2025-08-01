package com.messagesystem.backend.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.messagesystem.backend.contants.MessageType;

public class KeepAliveRequest extends BaseRequest {

    @JsonCreator
    public KeepAliveRequest() {
        super(MessageType.KEEP_ALIVE);
    }
}