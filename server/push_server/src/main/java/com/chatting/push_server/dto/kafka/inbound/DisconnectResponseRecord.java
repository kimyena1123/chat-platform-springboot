package com.chatting.push_server.dto.kafka.inbound;

import com.chatting.push_server.constant.MessageType;
import com.chatting.push_server.constant.UserConnectionStatus;
import com.chatting.push_server.dto.domain.UserId;


public record DisconnectResponseRecord(UserId userId, String username, UserConnectionStatus status) implements RecordInterface {

    @Override
    public String type() {
        return MessageType.DISCONNECT_RESPONSE;
    }
}