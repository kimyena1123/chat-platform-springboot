package com.chatting.push_server.dto.kafka.inbound;

import com.chatting.push_server.constant.MessageType;
import com.chatting.push_server.dto.domain.UserId;

/**
 * [ACCEPT_RESPONSE 응답용 레코드]
 * - "내가 수락했다"에 대한 응답 메시지 DTO
 * - RecordInterface 구현체(다형 역직렬화 대상)
 */
public record AcceptResponseRecord(UserId userId, String username) implements RecordInterface {
    @Override
    public String type() {
        return MessageType.ACCEPT_RESPONSE;
    }
}
