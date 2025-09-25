package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;

public class FetchChannelsListRequest extends BaseRequest{

    public FetchChannelsListRequest() {
        super(MessageType.FETCH_CHANNELS_LIST_REQUEST);

    }
}
