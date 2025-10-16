package com.chatting.push_server.handler.kafka;

import com.chatting.push_server.dto.kafka.inbound.AcceptNotificationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * [NOTIFY_ACCEPT 레코드 전용 핸들러]
 *
 * - "상대방이 초대를 수락했다"는 알림을 오프라인 사용자에게 푸시할 때 쓰이는 메시지.
 * - 여기서는 실제 푸시 전송 대신 "로깅"으로 대체(스켈레톤).
 *
 * 주의:
 * - 스프링 빈 등록을 위해 @Component가 필요함.
 *   (현재 코드엔 없었는데, Dispatcher가 자동 등록하려면 반드시 붙여야 함)
 */
@Slf4j
@Component
public class AcceptNotificationRecordHandler implements BaseRecordHandler<AcceptNotificationRecord> {

    @Override
    public void handleRecord(AcceptNotificationRecord record) {

        // 이 유저에게 메시지를 푸시했다는 로그
        // 여기서는 "오프라인 사용자에게 푸시했다"는 로그만 남긴다.
        // 실서비스에서는 APNs/Firebase 등 외부 푸시 게이트웨이 연동이 들어갈 자리.
        log.info("{} to offline userId: {}", record, record.userId());
    }
}
