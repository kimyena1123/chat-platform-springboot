package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.ChannelId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuitResponse extends BaseMessage{

    private final ChannelId channelId;

    @JsonCreator
    public QuitResponse(@JsonProperty("channelId") ChannelId channelId) {
        super(MessageType.QUIT_RESPONSE);
        this.channelId = channelId;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }
}
