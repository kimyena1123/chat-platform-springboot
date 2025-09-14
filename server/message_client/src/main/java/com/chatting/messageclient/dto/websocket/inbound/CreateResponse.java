package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.dto.domain.ChannelId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// =================================================================================================
// 역할: [서버 → 클라이언트] 채널 생성 성공 응답 메시지 DTO.
// 전송 대상:
//   - 요청자(채널 생성 요청을 보낸 클라이언트의 세션)
// =================================================================================================

public class CreateResponse extends BaseMessage{

    //client에게 전달할 정보: 채널id, title
    private final ChannelId channelId;  //생성된 채널 식별자
    private final String title;         //생성된 채널 제목

    @JsonCreator
    public CreateResponse(@JsonProperty("channelId") ChannelId channelId, @JsonProperty("title") String title) {
        super(MessageType.CREATE_RESPONSE);
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
