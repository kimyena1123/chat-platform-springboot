package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.Channel;

import java.util.List;

public class FetchChannelsListResponse extends BaseMessage{

    private final List<Channel> channels;

    public FetchChannelsListResponse(List<Channel> channels) {
        super(MessageType.FETCH_CHANNELS_LIST_RESPONSE);
        this.channels = channels;
    }

    //Getter
    public List<Channel> getChannels() {
        return channels;
    }
}
