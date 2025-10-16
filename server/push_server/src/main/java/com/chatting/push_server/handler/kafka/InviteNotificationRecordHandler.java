package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.InviteNotificationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InviteNotificationRecordHandler implements BaseRecordHandler<InviteNotificationRecord> {

    @Override
    public void handleRecord(InviteNotificationRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}