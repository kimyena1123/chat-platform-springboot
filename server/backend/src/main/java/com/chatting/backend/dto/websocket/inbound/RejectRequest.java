package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RejectRequest extends BaseRequest {

    //어떤 유저의 요청을 거절한건지 username으로 요청할 수 있다.
    //  로그인 할 때, email과 password가 필요 > request에 email, password
    //  채팅요청 거절, 누구의 요청 거절인지 이름일 필요 > 채팅 요청자(invitor)의 username 필요
    public final String username;

    @JsonCreator
    public RejectRequest(@JsonProperty("username") String username) {
        super(MessageType.REJECT_REQUEST);

        this.username = username;
    }

    public String getUsername() {
        return username;
    }

}
