package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;

public class FetchChannelsListRequest extends BaseRequest{

    @JsonCreator
    public FetchChannelsListRequest() {
        super(MessageType.FETCH_CHANNELS_LIST_REQUEST);

    }
}
