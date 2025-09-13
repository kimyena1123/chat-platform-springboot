package com.chatting.backend.constant;

public enum IdKey {

    HTTP_SESSION_ID("HTTP_SESSION_ID"),
    USER_ID("USER_ID"),
    CHANNEL_ID("channel_id"); //CHANNEL_ID가 저장될 곳이 redis에 저장할 거라서 redis는 소문자 권장이다.

    private final String value;

    IdKey(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}