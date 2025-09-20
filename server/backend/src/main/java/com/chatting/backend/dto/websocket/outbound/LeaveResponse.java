package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;

public class LeaveResponse extends BaseMessage{


    public LeaveResponse() {
        super(MessageType.LEAVE_RESPONSE);
    }

}
