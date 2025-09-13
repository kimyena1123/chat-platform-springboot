package com.chatting.backend.dto.websocket.inbound;


import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// =================================================================================================
// 역할: [클라이언트 → 서버] 특정 채널에 "입장"을 요청하는 메시지 DTO.
// 필드:
//   - channelId: 입장하려는 대상 채널 ID (ChannelId 타입)
// 흐름:
//   1) 클라이언트가 "ENTER_REQUEST" 전송
//   2) 서버에서 "해당 채널의 회원인지" 검사 후, 성공 시 세션의 활성 채널을 갱신(세션/Redis)
// =================================================================================================

public class EnterRequest extends BaseRequest {

    //어느 채널에 입장할건지 chanelId만 있어도 된다.
    private final ChannelId channelId;  // 입장할 채널 식별자

    @JsonCreator
    public EnterRequest(@JsonProperty("channelId") ChannelId channelId) {
        super(MessageType.ENTER_REQUEST);

        this.channelId = channelId;
    }

    //Getter
    public ChannelId getChannelId() {
        return channelId;
    }
}
