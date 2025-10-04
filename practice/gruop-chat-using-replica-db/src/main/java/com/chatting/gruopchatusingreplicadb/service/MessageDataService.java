package com.chatting.gruopchatusingreplicadb.service;

import com.chatting.gruopchatusingreplicadb.dto.Message;
import com.chatting.gruopchatusingreplicadb.entity.MessageEntity;
import com.chatting.gruopchatusingreplicadb.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDataService {

    private final MessageRepository messageRepository;

    @Transactional
    public void save(Message message, boolean makeException) {
        try {
            messageRepository.save(new MessageEntity(message.username(), message.content()));

            if (makeException) {
                throw new RuntimeException("For test");
            }

        } catch (Exception ex) {
            log.error("Message save failed. cause: {}", ex.getMessage());
            throw ex;
        }
    }
}