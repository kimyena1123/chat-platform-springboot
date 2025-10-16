package com.chatting.push_server.dto.kafka.inbound;

import com.chatting.push_server.constant.MessageType;
import com.chatting.push_server.dto.domain.ChannelId;
import com.chatting.push_server.dto.domain.UserId;

public record JoinNotificationRecord(UserId userId, ChannelId channelId, String title) implements RecordInterface {

    @Override
    public String type() {
        return MessageType.NOTIFY_JOIN;
    }
}