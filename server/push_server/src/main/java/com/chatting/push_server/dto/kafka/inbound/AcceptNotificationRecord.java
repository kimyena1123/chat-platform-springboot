package com.chatting.push_server.dto.kafka.inbound;

import com.chatting.push_server.constant.MessageType;
import com.chatting.push_server.dto.domain.UserId;

/**
 * [NOTIFY_ACCEPT 알림용 레코드]
 * - "상대방이 초대를 수락했다" 알리는 알림 메시지 DTO
 * - RecordInterface 구현체(다형 역직렬화 대상)
 */
public record AcceptNotificationRecord(UserId userId, String username) implements RecordInterface {

    @Override
    public String type() {
        return MessageType.NOTIFY_ACCEPT;
    }
}
