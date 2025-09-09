package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.InviteCode;

public class FetchUserInvitecodeResponse extends BaseMessage{

    private final InviteCode inviteCode;

    public FetchUserInvitecodeResponse(InviteCode inviteCode){
        super(MessageType.FETCH_USER_INVITECODE_RESPONSE);

        this.inviteCode = inviteCode;
    }

    public InviteCode getInviteCode() {
        return inviteCode;
    }
}
