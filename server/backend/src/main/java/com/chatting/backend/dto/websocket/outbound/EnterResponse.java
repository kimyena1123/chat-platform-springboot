package com.chatting.backend.dto.websocket.outbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;

// =================================================================================================
// 역할: [서버 → 클라이언트] 채널 입장 성공 응답 메시지 DTO.
// 전송 대상:
//   - 요청자(ENTER_REQUEST를 보낸 클라이언트의 세션)
// =================================================================================================

public class EnterResponse extends BaseMessage{

    //client에게 전달할 정보: 채널id, title
    private final ChannelId channelId;  //입장한 채널 식별자
    private final String title;         //채널 제목

    public EnterResponse(ChannelId channelId, String title) {
        super(MessageType.ENTER_RESPONSE);
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
