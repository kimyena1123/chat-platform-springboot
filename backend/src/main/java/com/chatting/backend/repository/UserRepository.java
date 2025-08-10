package com.chatting.backend.repository;

import com.chatting.backend.entity.UserEntity;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    //username(유저이름) 찾기
    //SELECT * FROM message_usr where username = ?
    Optional<UserEntity> findByUsername(@NonNull String username);
}
