package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.constant.UserConnectionStatus;

//내가 혆재 맺은 연결 중 특정 상태(PENDING, ACCEPTED 등)만 보고 싶을 때
public class FetchConnectionsRequest extends BaseRequest {

    //원하는 연결 상태(UserConnectionStatus)
    //서버가 해당 상태의 사람 목록을 찾아 클라이언트에 돌려주기 위해
    private final UserConnectionStatus status;

    public FetchConnectionsRequest(UserConnectionStatus status) {
        super(MessageType.FETCH_CONNECTIONS_REQUEST);
        this.status = status;
    }

    public UserConnectionStatus getStatus() {
        return status;
    }
}