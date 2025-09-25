package com.chatting.messageclient.dto.websocket.inbound;


import com.chatting.messageclient.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;

public class LeaveResponse extends BaseMessage{

    @JsonCreator
    public LeaveResponse() {
        super(MessageType.LEAVE_RESPONSE);
    }

}
