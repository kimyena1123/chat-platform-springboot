package com.chatting.messageclient.dto.websocket.inbound;


import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.ChannelId;
import com.chatting.messageclient.dto.domain.InviteCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FetchChannelInviteCodeResponse extends BaseMessage{

    //응답으로 어떤 채널의 초대코드를 주는건지 알려줘야 한다.
    private final ChannelId channelId;
    private final InviteCode inviteCode;

    @JsonCreator
    public FetchChannelInviteCodeResponse(@JsonProperty("channelId") ChannelId channelId, @JsonProperty("inviteCode")InviteCode inviteCode) {
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
