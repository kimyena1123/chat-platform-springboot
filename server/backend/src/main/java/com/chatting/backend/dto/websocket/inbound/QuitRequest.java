package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuitRequest extends BaseRequest{

    private final ChannelId channelId;


    @JsonCreator
    public QuitRequest(@JsonProperty("channelId") ChannelId channelId) {
        super(MessageType.QUIT_REQUEST);
        this.channelId = channelId;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }
}
