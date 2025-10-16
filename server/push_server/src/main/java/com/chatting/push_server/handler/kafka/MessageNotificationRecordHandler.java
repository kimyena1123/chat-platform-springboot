package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.MessageNotificationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageNotificationRecordHandler implements BaseRecordHandler<MessageNotificationRecord> {

    @Override
    public void handleRecord(MessageNotificationRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}