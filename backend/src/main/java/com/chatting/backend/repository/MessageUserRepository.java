package com.chatting.backend.repository;

import com.chatting.backend.entity.MessageUserEntity;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageUserRepository extends JpaRepository<MessageUserEntity, Long> {

    //username(유저이름) 찾기
    //SELECT * FROM message_usr where username = ?
    Optional<MessageUserEntity> findByUsername(@NonNull String username);
}
