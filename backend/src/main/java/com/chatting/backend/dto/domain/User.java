package com.chatting.backend.dto.domain;

import java.util.Objects;

public record User(UserId userId, String username) {

    //userId로만 비교하도록 재정의(원래 record로 만들면 기본적으로 equals, hashcode를 만들어준다)
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        User user = (User) object;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
