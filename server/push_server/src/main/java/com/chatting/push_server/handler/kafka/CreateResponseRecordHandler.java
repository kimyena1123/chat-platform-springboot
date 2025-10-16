package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.CreateResponseRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CreateResponseRecordHandler implements BaseRecordHandler<CreateResponseRecord> {

    @Override
    public void handleRecord(CreateResponseRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}
