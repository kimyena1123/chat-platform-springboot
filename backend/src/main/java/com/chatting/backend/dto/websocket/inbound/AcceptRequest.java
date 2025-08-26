package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.InviteCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AcceptRequest extends BaseRequest {

    //초대 승인 요청할 시 들어오는 정보 : username (어떤 사용자의 요청을 수락한 건지에 대한 정보가 필요하다)
    //즉 초대한 사람의 username
    private final String username;

    @JsonCreator
    public AcceptRequest(@JsonProperty("username") String username) {
        super(MessageType.ACCEPT_REQUEST);

        this.username = username;
    }

    //getter
    public String getUseranme() {
        return username;
    }
}

