package com.chatting.messageclient.dto.websocket.inbound;


import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.Channel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FetchChannelsListResponse extends BaseMessage{

    private final List<Channel> channels;

    @JsonCreator
    public FetchChannelsListResponse(@JsonProperty("channels") List<Channel> channels) {
        super(MessageType.FETCH_CHANNELS_LIST_RESPONSE);
        this.channels = channels;
    }

    //Getter
    public List<Channel> getChannels() {
        return channels;
    }
}
