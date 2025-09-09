package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 이 프로토콜 하나로 두 가지를 해결할 수 있다.
 * 1. 현재 대기 목록
 * 2. 연결된 목록
 */
public class FetchConnectionsRequest extends BaseRequest{

    //나와 어떤 연결 상태인지 확인하기 위한 status
    //내거 원하는 연결 상태에 대한 목록을 보고 싶은 것임.
    private final UserConnectionStatus status;

    @JsonCreator
    public FetchConnectionsRequest(@JsonProperty("status") UserConnectionStatus status) {
        super(MessageType.FETCH_CONNECTIONS_REQUEST);

        this.status = status;
    }

    //Getter
    public UserConnectionStatus getStatus() {
        return status;
    }
}
