package com.chatting.backend.dto.websocket.inbound;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.ChannelId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaveRequest extends BaseRequest{

    // 파라미터가 없다. 어떤 채널을 나갈건지 channelId를 알아야 하는 거 아닌가?
    // >> NO. leave는 특정 채팅방에 들어와 있을 때(enter)에만 사용 가능 하다.
    // 그렇기에 leave는 이미 내가 특정 채팅방에 있고, 해당 채팅방을 나가고 싶을 때 사용.

    //채팅방 나가기 : leave VS quit
    // leave : 해당 채팅방을 잠깐 나가는 것(참여자로 그대로 있음) - enter 할 때 등록했던 redis를 leave할 때 삭제해야 한다.
    // quit  : 해당 채팅방을 아예 나가는 것(참여자로 더이상 없음)

    @JsonCreator
    public LeaveRequest() {
        super(MessageType.LEAVE_REQUEST);
    }

}
