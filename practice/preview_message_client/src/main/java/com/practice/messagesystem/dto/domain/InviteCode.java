package com.practice.messagesystem.dto.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * domain 패키지: 내부 도메인 모델들(불변값 객체들)
 * 목적: 비즈니스 로지겡서 안전하고 명확하게 주고받을 수 있는 값 객체(value objects)를 정의
 *
 * - 불변(immutable)하며, 검증 로직(예. UserId가 양수읹, InviteCode가 비어있지 않은지)을 생성시점에 강제한다.
 *
 * [InviteCode]
 * 목적: 초대코드(사용자가 다른 사람을 초대할 때 주고받는 식별 문자열)를 표현
 */
public record InviteCode(@JsonValue String code) { //@JsonValue: 직렬화/역직렬화 시 단순 문자열로 다룸.

    @JsonCreator
    public InviteCode{
        if(code == null || code.isEmpty()){
            throw new IllegalArgumentException("Invalid InviteCode");
        }
    }
}
