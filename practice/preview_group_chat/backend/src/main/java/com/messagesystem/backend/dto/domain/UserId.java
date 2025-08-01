package com.messagesystem.backend.dto.domain;

public record UserId(Long id) {

    //생성자
    public UserId {
        if (id == null || id <= 0) {
            //예외 던지기
            throw new IllegalArgumentException("Invalid UserId");
        }
    }
}
