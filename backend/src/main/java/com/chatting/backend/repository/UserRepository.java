package com.chatting.backend.repository;

import com.chatting.backend.dto.projection.UsernameProjection;
import com.chatting.backend.entity.UserEntity;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    //username(유저이름) 찾기(username으로 해당 유저 정보 가져오기)
    //SELECT * FROM message_usr where username = ?
    Optional<UserEntity> findByUsername(@NonNull String username);

    //userId로 username 정보 가져오기
    Optional<UsernameProjection> findByUserId(@NonNull Long userId);

    //초대코드로 유저 찾기(초대코드로 유저 정보 가져오기)
    Optional<UserEntity> findByConnectionInviteCode(
            @NonNull String connectionInviteCode);
}
