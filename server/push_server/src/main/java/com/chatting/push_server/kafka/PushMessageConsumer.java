package com.chatting.push_server.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
/**
 * Kafka에서 "Consumer" 역할을 하는 부분"
 * producer가 메시지를 publish하면 해당 클래스(Consumer)가 subscribe해서 처리한다.
 */
public class PushMessageConsumer {

    @KafkaListener(
            topics = "${message-system.kafka.listeners.push.topic}",
            groupId = "${message-system.kafka.listeners.push.group-id}",
            concurrency = "${message-system.kafka.listeners.push.concurrency}")
    // 첫번쨰 파라미터: Consumer<Key, Value>
    // 두번째 파라미터: autocommit을 쓰면(기본 커밋을 쓰면) 두번째 파라미터를 안써도 되는데, 나는 수동으로 커밋을 찍을거라서 파라미터를 받아야 한다.
    public void consumeMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {

        //푸시 서버가 해주는 역할을 로깅이 전부 한다.
        //아래 로깅에 있는 정보는 다 "consumerRecord" 들어있다.
        //key는 설정을 안해둬서 null로 뜨지만 일단 명시해둔다.
        log.info("Received record: {}, from topic: {}, on key: {}, partition: {}, offset: {}",
                consumerRecord.value(), consumerRecord.topic(), consumerRecord.key(), consumerRecord.partition(), consumerRecord.offset());

        acknowledgment.acknowledge(); //commit을 찍는다.
    }
}
