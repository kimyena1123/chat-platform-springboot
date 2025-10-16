package com.chatting.push_server.dto.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * [값 객체: UserId]
 *
 * - null/음수 금지
 * - 직렬화 시 id 값만 나오도록 @JsonValue
 */
public record UserId(@JsonValue Long id) {

    //생성자
    public UserId{
        if(id == null || id < 0){
            //에외 던지기
            throw new IllegalArgumentException("Invalid UserId");
        }
    }
}
