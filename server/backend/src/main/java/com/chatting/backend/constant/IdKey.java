package com.chatting.backend.constant;

/**
 * 역할: WebSocket/HTTP 세션의 attribute 키 이름, 그리고 Redis 키 조립에 사용하는 identifier를 한 곳에 상수로 정의
 */
public enum IdKey {

    // 사용 예시:
    //   session.getAttributes().put(IdKey.USER_ID.getValue(), new UserId(123L));
    //   redis.set("message:user:123:channel_id", "456");
    HTTP_SESSION_ID("HTTP_SESSION_ID"), // WebSocket 핸드셰이크에서 HTTP 세션 식별자 저장 시 사용
    USER_ID("USER_ID"),                 // 현재 로그인한 사용자 식별자(UserId)를 세션 attribute로 저장할 때 사용
    CHANNEL_ID("channel_id");           // Redis에 "현재 사용자가 활성화해 둔 채널"을 기록할 때의 키 suffix로 사용(관례상 소문자)

    private final String value;

    IdKey(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}