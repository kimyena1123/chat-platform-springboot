package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.DisconnectResponseRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DisconnectResponseRecordHandler implements BaseRecordHandler<DisconnectResponseRecord> {

    @Override
    public void handleRecord(DisconnectResponseRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}