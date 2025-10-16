package com.chatting.push_server.dto.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * [값 객체: ChannelId]
 *
 * - 단순 Long을 굴리는 대신 "의미 있는 타입"으로 감싸 타입 안정성 ↑
 * - 생성 시 유효성 검사(<=0 금지)로 도메인 무결성 ↑
 * - @JsonValue: 직렬화 시 이 값을 그대로 사용
 * - @JsonCreator: 역직렬화 시 생성자 사용(검증 포함)
 */
public record ChannelId(@JsonValue Long id) {

    /** 생성 시 유효성 검사: null 또는 0 이하 금지 */
    @JsonCreator
    public ChannelId{
        if(id == null || id <= 0){
            throw new IllegalArgumentException("Invalid UserChannelId");
        }
    }
}
