package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.AcceptResponseRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * [ACCEPT_RESPONSE 레코드 전용 핸들러]
 *
 * - "내가 수락 버튼을 눌렀을 때" 나에게 돌아오는 응답을 오프라인 상태면 푸시하는 용도.
 * - 역시 현재는 로깅으로 대체.
 */
@Slf4j
@Component
public class AcceptResponseRecordHandler implements BaseRecordHandler<AcceptResponseRecord> {

    @Override
    public void handleRecord(AcceptResponseRecord record) {
        log.info("{} to offline userId: {}", record, record.userId());
    }
}