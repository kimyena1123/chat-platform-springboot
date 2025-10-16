package com.chatting.push_server.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component

/** "리밸런싱(Rebalance)" 이벤트를 감지해서 로깅하는 리스너.
 *
 * # 1. 리밸런싱(Rebalancing)이란?
 * - 같은 group.id를 가진 여러 Consumer(소비자)들이 협업할 때, "누가 어떤 파티션을 읽을지" 배분이 필요하다.
 * - Consumer가 늘거나 줄거나, 파티션 수가 변하면 "분배"를 다시 해야 한다. > 이 재분배 과정이 "리밸런싱"이다.
 *
 * # 2. TopicPartition이란?
 * - 카프카는 하나의 "Topic"이 여러 "Partition"으로 쪼개져 있다.
 * - 각 파티션은 "순서가 보장되는 로그"이다.
 * - TopicPartition은ㅇ (토픽명, 파티션 번호)를 나타내는 식별자이다.
 *
 * [활용예시]
 * - 회수 시점에 미처 처리 못한 메시지를 마무리하거나, 수동 커밋 모드인 경우 마지막 오프셋을 커밋하는 등의 정리 로직을 둘 수 있다
 *
 */
public class KafkaConsumerAwareRebalanceListener implements ConsumerAwareRebalanceListener {


    /** [파티션이 새로 "할당"되었을 때 호출]
     *
     * @param consumer      실제 카프카 Consumer 객체(오프셋 관리, 폴링 등 담당)
     * @param partitions    이 컨슈머에게 새로 배정된 TopicPartition 목록
     */
    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
//        ConsumerAwareRebalanceListener.super.onPartitionsAssigned(consumer, partitions);
        log.info("Kafka consumer {} assigned: {}", consumer.toString(), partitions.toString());
    }


    /** [파티션이 "회수"(더이상 내가 읽지 않게) 될 때 호출]
     *
     * @param partitions    이 컨슈머로부터 회수된 TopicPartition 목록
     */
    @Override
     public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
//        ConsumerAwareRebalanceListener.super.onPartitionsRevoked(partitions);
        log.info("Kafka consumer revoked: {}", partitions.toString());
    }
}
