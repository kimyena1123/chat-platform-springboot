package com.chatting.push_server.kafka;

import com.chatting.push_server.dto.kafka.inbound.RecordInterface;
import com.chatting.push_server.handler.RecordDispatcher;
import com.chatting.push_server.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * [Kafka Consumer의 진입점]
 *
 * - 카프카 토픽으로부터 메시지를 "소비"하는 클래스.
 * - Producer(채팅 서버)가 push_server에게 보낸 JSON 메시지를 받아서
 *   1) JSON -> 다형 타입(RecordInterface 하위 타입)으로 역직렬화
 *   2) 레코드 타입에 맞는 "핸들러"로 디스패치(RecordDispatcher)
 *   3) 처리 이후 오프셋(Offset) 커밋(수동 커밋)
 *
 * 왜 필요한가?
 * - push_server는 "오프라인 사용자에게 푸시"를 담당하는 마이크로서비스.
 * - 채팅 서버는 메시지를 카프카 토픽에 publish하고,
 *   이 Consumer가 그 토픽을 구독하여 수신/처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushMessageConsumer {

    private final RecordDispatcher recordDispatcher;
    private final JsonUtil jsonUtil;


    /**
     * [카프카 리스너]
     * - @KafkaListener가 붙은 메서드는 Spring이 백그라운드 스레드에서 실행한다.
     * - topics: 구독할 토픽명(설정값 주입)
     * - groupId: 컨슈머 그룹 ID(같은 그룹에 속한 컨슈머끼리 파티션을 나눠 읽음)
     * - concurrency: 동일한 @KafkaListener 메서드의 동시 실행 스레드 수(파티션 병렬 처리)
     *
     * 파라미터 설명:
     * - consumerRecord: 카프카가 넘겨주는 "레코드(메시지 1건)"의 메타데이터 + 페이로드(value)
     *      Consumer<Key, Value>
     * - acknowledgment: 수동 커밋을 위해 사용하는 객체(acknowledge() 호출 시 즉시 커밋)
     *      두번째 파라미터: autocommit을 쓰면(기본 커밋을 쓰면) 두번째 파라미터를 안써도 되는데, 나는 수동으로 커밋을 찍을거라서 파라미터를 받아야 한다.
     *
     * 주의:
     * - 커밋 타이밍을 우리가 제어(수동 커밋)하므로, "정말 처리 완료" 시점에만 커밋해야 재처리/유실을 통제할 수 있다.
     */
    @KafkaListener(
            topics = "${message-system.kafka.listeners.push.topic}",
            groupId = "${message-system.kafka.listeners.push.group-id}",
            concurrency = "${message-system.kafka.listeners.push.concurrency}")
    public void consumeMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {

        try {
            //푸시 서버가 해주는 역할을 로깅이 전부 한다.
            //아래 로깅에 있는 정보는 다 "consumerRecord" 들어있다.
            //key는 설정을 안해둬서 null로 뜨지만 일단 명시해둔다.
            log.info("Received record: {}, from topic: {}, on key: {}, partition: {}, offset: {}",
                    consumerRecord.value(), consumerRecord.topic(), consumerRecord.key(), consumerRecord.partition(), consumerRecord.offset());

            // 2) JSON → 다형 타입 역직렬화
            //    - 핵심 포인트: JSON 안에 "type" 필드가 있고,
            //      그 값에 따라 RecordInterface의 하위 타입(예: AcceptResponseRecord)으로 자동 매핑된다.
            //    - fromJson(...)은 Optional을 반환하므로, 값이 있을 때만 dispatch
            jsonUtil.fromJson(consumerRecord.value(), RecordInterface.class).ifPresent(recordDispatcher::dispatchRecord);
        } catch (Exception ex) {
            // 3) 처리 중 예외 로깅
            //    - 현재 코드는 에러가 나도 finally에서 커밋한다(=재처리하지 않음).
            //    - 실서비스에서는 "실패 시 커밋하지 않기" or "DLQ로 보내기" 등 전략을 택할 수 있다.
            log.error("Record handling failed. cause: {}", ex.getMessage());
        } finally {
            // 4) 오프셋 수동 커밋
            //    - AckMode.MANUAL_IMMEDIATE 설정(컨테이너)과 짝을 이룬다.
            //    - finally에서 commit → 실패여도 커밋됨(현재 코드 정책).
            //    - 운영 정책에 따라 성공시에만 커밋하도록 바꿀 수 있다.
            acknowledgment.acknowledge(); //commit을 찍는다.
        }


    }
}
