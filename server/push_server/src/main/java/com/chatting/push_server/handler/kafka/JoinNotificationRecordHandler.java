package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.JoinNotificationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JoinNotificationRecordHandler implements BaseRecordHandler<JoinNotificationRecord> {

    @Override
    public void handleRecord(JoinNotificationRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}
