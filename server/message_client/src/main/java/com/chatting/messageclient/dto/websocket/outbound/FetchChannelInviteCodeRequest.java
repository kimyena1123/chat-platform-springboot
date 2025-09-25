package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.ChannelId;

public class FetchChannelInviteCodeRequest extends BaseRequest{

    private final ChannelId channelId;

    public FetchChannelInviteCodeRequest(ChannelId channelId){
        super(MessageType.FETCH_CHANNEL_INVITECODE_REQUEST);
        this.channelId = channelId;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }
}
