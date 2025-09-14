package com.chatting.backend.dto.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ChannelId(@JsonValue Long id) {

    /** 생성 시 유효성 검사: null 또는 0 이하 금지 */
    @JsonCreator
    public ChannelId{
        if(id == null || id <= 0){
            throw new IllegalArgumentException("Invalid UserChannelId");
        }
    }
}
