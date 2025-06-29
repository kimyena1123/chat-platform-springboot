package com.study.integrationtest.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

//Kafka로 알림 전송
@Service
public class PushService {

    private final String topicName = "push.notification"; // Kafka 토픽 이름

    private final KafkaTemplate<String, String> kafkaTemplate;

    // 생성자 주입 (KafkaTemplate 주입)
    public PushService(KafkaTemplate<String, String> kafkaTemplate){

        this.kafkaTemplate = kafkaTemplate;
    }

    /* [알림 메시지를 Kafka로 전송하는 메서드]
       Kafka는 메시지를 보내는 메시지 브로커(메신저 같은 역할)
       notification("대출 완료")를 호출하면, Kafka가 다른 시스템으로 그 메시지를 전송해줍니다.
     */
    public void notification(String message){
        kafkaTemplate.send(topicName, message);
    }
}
