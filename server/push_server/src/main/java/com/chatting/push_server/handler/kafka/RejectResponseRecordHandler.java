package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.RejectResponseRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RejectResponseRecordHandler implements BaseRecordHandler<RejectResponseRecord> {

    @Override
    public void handleRecord(RejectResponseRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}