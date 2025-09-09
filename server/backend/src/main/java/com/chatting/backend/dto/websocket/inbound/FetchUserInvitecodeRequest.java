package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;

public class FetchUserInvitecodeRequest extends BaseRequest{

    //이 request는 현재 자신 걸 확인하는 request이고 세션이 연결된 상태에서 올라오는 것이기에 내가 나 자신이라는 걸 입증할 필요가 없다.
    //그래서 request가 올라오면 그냥 response를 주면 된다

    @JsonCreator
    public FetchUserInvitecodeRequest(){
        super(MessageType.FETCH_USER_INVITECODE_REQUEST);
    }
}
