package com.chatting.backend.dto.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record InviteCode(@JsonValue String code) {

    //이상한 값이 들어오지 않도록 방어
    @JsonCreator
    public InviteCode{
        if(code == null || code.isEmpty()){
            throw new IllegalArgumentException("invalid InviteCode");
        }
    }
}
