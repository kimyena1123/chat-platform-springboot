package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;

//주의) "OOO남이 새로운 메시지를 보냈습니다"와 같은 푸시 알림 같은 기능을 하는게 아니다.
//사용자에게 전송한 메시지를 보여주는 역할을 한다.
public class MessageNotification extends BaseMessage {

    //어떤 사용자 어떤 채널에 어떤 메시지를 보냈는지에 대한 알림
    private final ChannelId channelId;
    private final String username;
    private final String content;

    public MessageNotification(ChannelId channelId,String username, String content) {
        super(MessageType.NOTIFY_MESSAGE);
        this.channelId = channelId;
        this.username = username;
        this.content = content;
    }

    public ChannelId getChannelId() {
        return channelId;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }
}