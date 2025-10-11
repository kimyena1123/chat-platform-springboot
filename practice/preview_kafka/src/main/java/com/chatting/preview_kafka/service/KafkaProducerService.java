package com.chatting.preview_kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, String key, String message) {
        // return 타입 만들어야 하니까 호출
        // 기본(원래가)이 비동기라서 Future를 리턴한다. 하지만 나는 비동기로 쓸 건 아니라서
        SendResult<String, String> sendResult;

        //try{}로 감싸는 이유: Kafka를 프로듀싱할 때 "동기" 방식으로 던질거다.
        //원래는 비동기로 동작하는건데 우리의 요구사항에서 동기 방식이 더 적합하다. 왜??
        try{

            // key가 있는 것과 없는 것의 동작이 다르다.
            // 1. key 있음: 파티션이 여러 개 있을 때, 해당 파티션으로만 간다. 같은 키는 샅은 파티션으로 간다. 키가 다르면 다른 파티션으로 간다.
            // 2. key 없음: 카프카가 프로듀서에 세팅되어 있는 기본 룰에 따라서 동작한다
            if(key == null || key.isEmpty()){
                sendResult = kafkaTemplate.send(topic, message).get();
            } else{
                //바로 받을려면 send()에서 get()으로 바로 블로킹 모드로 들어가서 대기를 타야 한다. 왜??
                sendResult = kafkaTemplate.send(topic, key, message).get();
            }

            log.info("Send result: {}", sendResult);

        } catch (Exception ex){
            log.info("Send failed: {} to topic: {}, key: {}, cause: {}", message, topic, key, ex.getMessage());
        }
    }
}
