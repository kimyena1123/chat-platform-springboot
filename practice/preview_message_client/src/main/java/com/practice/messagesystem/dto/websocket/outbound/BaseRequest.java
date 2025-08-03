package com.practice.messagesystem.dto.websocket.outbound;


//직렬활 할 때 필요하지 직렬화할 땐 사용X(JSON으로 직렬화할 땐 필요X)
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = MessageRequest.class, name = MessageType.MESSAGE),
//        @JsonSubTypes.Type(value = KeepAliveRequest.class, name = MessageType.KEEP_ALIVE)
//})
public abstract class BaseRequest {
    private final String type;

    public BaseRequest(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}