package com.chatting.push_server.config;

import com.chatting.push_server.kafka.KafkaConsumerAwareRebalanceListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Slf4j
@Configuration
public class KafkaConfig {


    /**
     * 스프링 카프카가 @KafkaListener 메서드를 백그라운드 스레드에서 돌리기 위해
     * "리스너 컨테이너"를 만듭니다. 이 컨테이너의 실행 정책을 여기서 정의
     *
     * - ConsumerFactory: 실제 카프카 Consumer 클라이언트를 만드는 팩토리(부트 설정 사용)
     * - KafkaConsumerAwareRebalanceListener: 리밸런싱(파티션 재분배) 이벤트 콜백
     *
     * 반환: ConcurrentKafkaListenerContainerFactory
     *  => 스프링이 이 팩토리를 기반으로 @KafkaListener 컨테이너(스레드/풀)를 생성.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaConsumerAwareRebalanceListener awareRebalanceListener
    ) {
        // 컨테이너 팩토리 생성
        ConcurrentKafkaListenerContainerFactory<String, String> containerFactory = new ConcurrentKafkaListenerContainerFactory<>();

        // 1) 컨슈머 팩토리 주입: 부트의 spring.kafka.consumer.* 설정을 사용해 Consumer를 생성
        containerFactory.setConsumerFactory(consumerFactory);

        // 2) Ack 모드 설정: "수동 즉시 커밋"
        //    - 기본(자동) 커밋 X. 리스너 코드에서 acknowledgment.acknowledge()를 호출하는 "그 순간" 커밋
        //    - 왜? 푸시 성공 시점에만 커밋하고 싶기 때문(유실/중복을 우리 쪽에서 통제)
        containerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); //명시적으로 호출한 그 순간에 바로 커밋시키도록 한다

        // 3) 리밸런스 리스너 등록
        //    - 컨슈머 그룹의 리밸런싱이 일어날 때(파티션 회수/배정) 콜백을 받아 로깅/정리
        containerFactory.getContainerProperties().setConsumerRebalanceListener(awareRebalanceListener);

        log.info("Set AckMode: {}", containerFactory.getContainerProperties().getAckMode());
        return containerFactory;

    }

}
