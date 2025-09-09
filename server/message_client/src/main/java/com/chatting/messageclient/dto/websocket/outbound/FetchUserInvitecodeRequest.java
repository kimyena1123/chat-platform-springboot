package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;

//내가 내 초대코드가 무엇인지 확인하고 싶을 때
public class FetchUserInvitecodeRequest extends BaseRequest {

    //담는 정보: 없음(내 세션으로 식별 가능)
    //이유: 서버가 나의 초대코드를 생성/조회해 돌려줌

    public FetchUserInvitecodeRequest() {
        super(MessageType.FETCH_USER_INVITECODE_REQUEST);
    }
}