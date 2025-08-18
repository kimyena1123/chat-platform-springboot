package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.InviteCode;

public class InviteResponse extends BaseMessage{

    //어떤 초대코드로 결과가 전달됐는지 응답을 줘야 하기에 inviteCode 사용
    private final InviteCode inviteCode;

    //pending으로 온 건지, accepted로 온 건지 그런 상태값을 응답으로 줄 거다.
    private final UserConnectionStatus userConnectionStatus;

    public InviteResponse(InviteCode inviteCode, UserConnectionStatus userConnectionStatus) {
        super(MessageType.INVITE_RESPONSE);

        this.inviteCode = inviteCode;
        this.userConnectionStatus = userConnectionStatus;
    }

    //getter
    public InviteCode getInviteCode() {
        return inviteCode;
    }

    public UserConnectionStatus getUserConnectionStatus() {
        return userConnectionStatus;
    }
}
