package com.chatting.backend.dto.domain;

public record ChannelId(Long id) {

    /** 생성 시 유효성 검사: null 또는 0 이하 금지 */
    public ChannelId{
        if(id == null || id <= 0){
            throw new IllegalArgumentException("Invalid UserChannelId");
        }
    }
}
