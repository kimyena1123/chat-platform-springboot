package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;

public class ErrorResponse extends BaseMessage{

    private String messageType;
    private String message;

    public ErrorResponse(String messageType, String message) {
        super(MessageType.ERROR);

        this.messageType = messageType;
        this.message = message;
    }

    //getter
    public String getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }
}

