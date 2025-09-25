package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;

public class LeaveRequest extends BaseRequest{

    public LeaveRequest() {
        super(MessageType.LEAVE_REQUEST);
    }

}
