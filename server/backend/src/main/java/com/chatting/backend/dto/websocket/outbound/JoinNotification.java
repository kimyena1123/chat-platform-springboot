package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;

/**
 * 1:1 채팅이든, 그룹 채팅이든 "채팅방 생성(개설)"되면
 * 채팅방 개설자에게는 채팅방 생성에 대한 응답인 CreateResponse를 보내고
 * 채팅방 참여자들에게는 "채팅방에 가입되었다는; 채팅방이 생성되었다는" 알림인 JoinNotification을 보내야 한다.
 *
 * 그래서, JoinNotification(channelId, title)로 보내는데, 참여자들에게 어떤 채널의 어떤 채널명에 가입되었는지 알려주기 위함이다.
 */
public class JoinNotification extends BaseMessage{

    //어떤 채널의 어떤 채널명에 해당 알림을 보낼건지 알려주기 위함
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
