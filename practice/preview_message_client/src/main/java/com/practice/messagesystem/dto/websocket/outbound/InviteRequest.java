package com.practice.messagesystem.dto.websocket.outbound;

import com.practice.messagesystem.contants.MessageType;
import com.practice.messagesystem.dto.domain.InviteCode;

//내가 다른 사람에게 채팅을 초대하고 싶을 때
public class InviteRequest extends BaseRequest {


    //상대방(초대 받는자)의 inviteCode
    //초대는 상대방을 특정해야 하므로 상대방의 초대코드 필요
    private final InviteCode userInviteCode;

    public InviteRequest(InviteCode userInviteCode) {
        super(MessageType.INVITE_REQUEST);
        this.userInviteCode = userInviteCode;
    }

    //Getter
    public InviteCode getUserInviteCode() {
        return userInviteCode;
    }
}
