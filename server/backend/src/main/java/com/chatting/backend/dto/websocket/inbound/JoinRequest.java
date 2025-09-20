package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.InviteCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinRequest extends BaseRequest{

    //내가 참여되어 있지 않은 단톡방 or 채팅방에 들어가기 위해서 입력해야 하는 것 : 채널의 invitecode
    private final InviteCode inviteCode;

    @JsonCreator
    public JoinRequest(@JsonProperty("inviteCode") InviteCode inviteCode) {
        super(MessageType.JOIN_REQUEST);
        this.inviteCode = inviteCode;
    }

    //Getter
    public InviteCode getInviteCode() {
        return inviteCode;
    }
}
