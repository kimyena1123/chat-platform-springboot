package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.InviteResponseRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InviteResponseRecordHandler implements BaseRecordHandler<InviteResponseRecord> {

    @Override
    public void handleRecord(InviteResponseRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}