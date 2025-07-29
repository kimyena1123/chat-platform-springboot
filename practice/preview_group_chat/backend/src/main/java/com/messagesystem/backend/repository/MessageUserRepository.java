package com.messagesystem.backend.repository;

import com.messagesystem.backend.entity.MessageUserEntity;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface MessageUserRepository extends JpaRepository<MessageUserEntity, Long> {

    //user 이름으로 찾기
    //select * from message_user where username = ?
    Optional<MessageUserEntity> findByUsername(@NonNull String username);
}
