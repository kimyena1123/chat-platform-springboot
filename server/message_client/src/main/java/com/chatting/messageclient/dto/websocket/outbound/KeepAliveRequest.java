package com.chatting.messageclient.dto.websocket.outbound;

import com.chatting.messageclient.constant.MessageType;

//클라이언트가 "아직 살아있어, 연결 유지 중이야"라고 서버에 신호를 보낼 때
public class KeepAliveRequest extends BaseRequest {

    //담는 정보: 없음
    //이유: 서버에서 유휴 연결을 끊지 않고 유지할 수 있도록

    public KeepAliveRequest() {
        super(MessageType.KEEP_ALIVE);
    }
}