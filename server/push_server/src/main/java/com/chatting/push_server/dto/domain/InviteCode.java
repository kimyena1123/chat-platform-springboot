package com.chatting.push_server.dto.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * [값 객체: InviteCode]
 *
 * - 빈 문자열/NULL 금지
 * - 프런트/서버 경계를 넘어다니는 값이므로 생성 시 강력한 방어코드가 유리
 */
public record InviteCode(@JsonValue String code) {

    //이상한 값이 들어오지 않도록 방어
    @JsonCreator
    public InviteCode{
        if(code == null || code.isEmpty()){
            throw new IllegalArgumentException("invalid InviteCode");
        }
    }
}
