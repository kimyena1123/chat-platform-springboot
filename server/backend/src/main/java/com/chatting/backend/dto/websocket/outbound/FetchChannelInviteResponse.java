package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.domain.InviteCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FetchChannelInviteResponse extends BaseMessage{

    //응답으로 어떤 채널의 초대코드를 주는건지 알려줘야 한다.
    private final ChannelId channelId;
    private final InviteCode inviteCode;

    public FetchChannelInviteResponse(ChannelId channelId, InviteCode inviteCode) {
        super(MessageType.FETCH_CHANNEL_INVITECODE_RESPONSE);
        this.channelId = channelId;
        this.inviteCode = inviteCode;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }

    public InviteCode getInviteCode() {
        return inviteCode;
    }
}
