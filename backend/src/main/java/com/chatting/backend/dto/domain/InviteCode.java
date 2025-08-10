package com.chatting.backend.dto.domain;

public record InviteCode(String code) {

    //이상한 값이 들어오지 않도록 방어
    public InviteCode{
        if(code == null || code.isEmpty()){
            throw new IllegalArgumentException("invalid InviteCode");
        }
    }
}
