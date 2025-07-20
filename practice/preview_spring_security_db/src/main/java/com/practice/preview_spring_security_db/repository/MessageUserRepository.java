package com.practice.preview_spring_security_db.repository;

import com.practice.preview_spring_security_db.entity.MessageUserEntity;
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
