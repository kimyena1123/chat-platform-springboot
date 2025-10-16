package com.chatting.push_server.dto.kafka.inbound;

import com.chatting.push_server.constant.MessageType;
import com.chatting.push_server.constant.UserConnectionStatus;
import com.chatting.push_server.dto.domain.InviteCode;
import com.chatting.push_server.dto.domain.UserId;

public record InviteResponseRecord(UserId userId, InviteCode inviteCode, UserConnectionStatus status) implements RecordInterface {

    @Override
    public String type() {
        return MessageType.INVITE_RESPONSE;
    }
}