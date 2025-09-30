package com.chatting.gruopchatusingreplicadb.repository;

import com.chatting.gruopchatusingreplicadb.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {}