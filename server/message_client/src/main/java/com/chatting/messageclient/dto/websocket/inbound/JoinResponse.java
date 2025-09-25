package com.chatting.messageclient.dto.websocket.inbound;


import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.ChannelId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinResponse extends BaseMessage{

    //채널에 가압하면 어떤 채널에 어떤 채널명인지 알려주기 위함
    private final ChannelId channelId;
    private final String title;

    @JsonCreator
    public JoinResponse(@JsonProperty("channelId") ChannelId channelId, @JsonProperty("title") String title) {
        super(MessageType.JOIN_RESPONSE);
        this.channelId = channelId;
        this.title = title;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }

    public String getTitle() {
        return title;
    }
}
