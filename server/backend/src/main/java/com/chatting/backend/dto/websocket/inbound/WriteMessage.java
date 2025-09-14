package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WriteMessage extends BaseRequest {

//    private final String username;
    private final ChannelId channelId;
    private final String content;

    @JsonCreator
    public WriteMessage(
            @JsonProperty("channelId") ChannelId channelId, @JsonProperty("content") String content) {
        super(MessageType.WRITE_MESSAGE);
        this.channelId = channelId;
        this.content = content;
    }

    public ChannelId getChannelId() {
        return channelId;
    }

    public String getContent() {
        return content;
    }
}