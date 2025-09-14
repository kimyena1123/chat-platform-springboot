package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.ChannelId;

//내가 채팅방에서 메시지를 보낼 때
public class WriteMessage extends BaseRequest {

    //서버가 메시지를 전달하면서 "누가 보냈는지, 무슨 내용인지"를 알아야 함
    private final ChannelId channelId;
    private final String content;   //메시지의 실제 내용

    public WriteMessage(ChannelId channelId, String content) {
        super(MessageType.WRITE_MESSAGE);

        this.channelId = channelId;
        this.content = content;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }


    public String getContent() {
        return content;
    }
}