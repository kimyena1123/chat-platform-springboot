package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.Connection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** [FetchConnectionsResponse (MessageType.FETCH_CONNECTIONS_RESPONSE)]
 *
 * 언제: 클라이언트가 FetchConnectionsRequest(특정 상태의 연결 목록 요청)를 보냈을 때 서버가 회신.
 * 누가 받나: 요청자(자기 자신).
 * 필드: connections — List<Connection> (각 항목에 username + status).
 * 클라이언트 처리: 목록 렌더링(친구/대기/거절 목록 등).
 */
public class FetchConnectionsResponse extends BaseMessage {

    private final List<Connection> connections;

    @JsonCreator
    public FetchConnectionsResponse(@JsonProperty("connections") List<Connection> connections) {
        super(MessageType.FETCH_CONNECTIONS_RESPONSE);
        this.connections = connections;
    }

    public List<Connection> getConnections() {
        return connections;
    }
}