package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;

public class QuitResponse extends BaseMessage{

    private final ChannelId channelId;

    public QuitResponse(ChannelId channelId) {
        super(MessageType.QUIT_RESPONSE);
        this.channelId = channelId;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }
}
