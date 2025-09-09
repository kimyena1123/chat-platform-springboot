package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.InviteCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InviteRequest extends BaseRequest {

    private final InviteCode userInviteCode;


    @JsonCreator
    public InviteRequest(@JsonProperty("userInviteCode") InviteCode userInviteCode) {
        super(MessageType.INVITE_REQUEST);

        this.userInviteCode = userInviteCode;
    }


    //getter
    public InviteCode getUserInviteCode() {
        return userInviteCode;
    }
}

