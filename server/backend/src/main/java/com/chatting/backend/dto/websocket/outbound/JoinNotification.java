package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;

public class JoinNotification extends BaseMessage{

    //client에게 전달할 정보: 채널id, title
    private final ChannelId channelId;
    private final String title;

    public JoinNotification(ChannelId channelId, String title) {
        super(MessageType.NOTIFY_JOIN);
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
