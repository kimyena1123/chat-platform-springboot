package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.QuitResponseRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuitResponseRecordHandler implements BaseRecordHandler<QuitResponseRecord> {

    @Override
    public void handleRecord(QuitResponseRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}