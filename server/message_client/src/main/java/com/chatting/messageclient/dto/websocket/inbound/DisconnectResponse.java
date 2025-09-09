package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.constant.UserConnectionStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** [DisconnectResponse (MessageType.DISCONNECT_RESPONSE)]
 *
 * 언제: 사용자가 연결 끊기(DisconnectRequest)를 요청했을 때 서버가 요청자에게 회신하거나, 또는 다른 이유로 서버가 클라이언트에게 끊김 상태를 알릴 때.
 * 누가 받나: 요청자(끊은 사람) 또는 관련된 당사자(설계에 따라).
 * 필드: username (상대방 이름), status (새 상태, 예: DISCONNECTED).
 * 클라이언트 처리: 로컬에서 연결 목록 업데이트(ACCEPTED→DISCONNECTED), UI 알림.
 */
public class DisconnectResponse extends BaseMessage {

    private final String username;
    private final UserConnectionStatus status;

    @JsonCreator
    public DisconnectResponse(
            @JsonProperty("username") String username,
            @JsonProperty("status") UserConnectionStatus status) {
        super(MessageType.DISCONNECT_RESPONSE);
        this.username = username;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public UserConnectionStatus getStatus() {
        return status;
    }
}