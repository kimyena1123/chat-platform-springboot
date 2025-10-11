package com.chatting.preview_kafka.controller;

import com.chatting.preview_kafka.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka")
@RequiredArgsConstructor
public class KafkaController {

    private final KafkaProducerService producerService;

    @PostMapping("/send")
    public void sendMessage(@RequestParam String topic, @RequestParam(required = false) String key, @RequestParam String message){
        producerService.sendMessage(topic, key, message);
    }
}
