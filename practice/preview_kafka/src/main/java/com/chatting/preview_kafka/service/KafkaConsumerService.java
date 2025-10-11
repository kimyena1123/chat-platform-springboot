package com.chatting.preview_kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    /**
     *
     * @param message message를 받아서 consuming 할거다.
     */
    //Consuming 그룹이다. 같은 그룹은 같은 이름을 써야 그 그룹의 consumer로서 참여할 수 있다.
    @KafkaListener(topics = "test-topic", groupId = "test-group")
    public void consume(String message){
        log.info("Consumed message: {}", message);
    }

}
