package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaveRequest extends BaseRequest{


    @JsonCreator
    public LeaveRequest() {
        super(MessageType.LEAVE_REQUEST);
    }

}
