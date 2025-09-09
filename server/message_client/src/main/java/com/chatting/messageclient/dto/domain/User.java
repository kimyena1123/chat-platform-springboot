package com.chatting.messageclient.dto.domain;

import java.util.Objects;

/**
 * domain 패키지: 내부 도메인 모델들(불변값 객체들)
 * 목적: 비즈니스 로지겡서 안전하고 명확하게 주고받을 수 있는 값 객체(value objects)를 정의
 *
 * - 불변(immutable)하며, 검증 로직(예. UserId가 양수읹, InviteCode가 비어있지 않은지)을 생성시점에 강제한다.
 *
 * [User]
 * 목적: 간단한 사용자 도메인 DTO (식별자와 보여줄 이름)
 */
public record User(UserId userId, String username) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}