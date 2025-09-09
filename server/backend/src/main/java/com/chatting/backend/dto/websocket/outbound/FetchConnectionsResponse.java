package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.Connection;

import java.util.List;

public class FetchConnectionsResponse extends BaseMessage{

    //목록이니까 List 사용
    //Connection: username, status
    private final List<Connection> connections;

    public FetchConnectionsResponse(List<Connection> connections){
        super(MessageType.FETCH_CONNECTIONS_RESPONSE);
        this.connections = connections;
    }

    //Getter
    public List<Connection> getConnections() {
        return connections;
    }
}
