package com.chatting.gruopchatusingreplicadb.repository;

import com.chatting.gruopchatusingreplicadb.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    //최신 메시지 찾기
    //TOP: 최신(LIMIT 1의 의미를 한다)
    @Transactional(readOnly = true)
    Optional<MessageEntity> findTopByOrderByMessageSequenceDesc();
}