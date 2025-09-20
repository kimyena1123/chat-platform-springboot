package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;

public class JoinResponse extends BaseMessage{

    //채널에 가압하면 어떤 채널에 어떤 채널명인지 알려주기 위함
    private final ChannelId channelId;
    private final String title;

    public JoinResponse(ChannelId channelId, String title) {
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
