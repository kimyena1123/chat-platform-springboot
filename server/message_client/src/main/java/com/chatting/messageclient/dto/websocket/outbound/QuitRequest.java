package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.ChannelId;

public class QuitRequest extends BaseRequest{

    private final ChannelId channelId;

    public QuitRequest(ChannelId channelId) {
        super(MessageType.QUIT_REQUEST);
        this.channelId = channelId;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }
}
