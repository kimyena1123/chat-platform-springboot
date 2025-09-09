package com.chatting.backend.dto.websocket.outbound;

/**
 * inbound의 BaseRequest와 같이 outbound의 BaseResponse라고 생각하면 된다.
 * 중립적인 이름을 붙임
 */
public abstract class BaseMessage {

    private final String type;

    public BaseMessage(String type) {
        this.type = type;
    }

    //getter
    public String getType() {
        return type;
    }
}
