package com.chatting.backend.repository;

import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.projection.UsernameProjection;
import com.chatting.backend.entity.UserEntity;
import jakarta.persistence.LockModeType;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // connection_count 정보를 업데이트 하는 과정에서 동시성 문제가 발생할 수 있다 > rock 설정
    //동시성 문제란, 여러 개의 트랜잭션(요청)이 동시에 같은 데이터를 읽고 수정할 때 발생하는 문제
    //  - UserEntity에 connectionCount(연결 수)가 있다고 하자. 두 명의 사용자가 동시에 같은 사람을 초대하려고 할 때,
    //      트랜잭션 A: connectionCount를 읽음 -> 값은 10
    //      트랜잭션 B: connectionCount를 읽음 -> 값은 10
    //      A가 connectionCount++ 해서 11로 저장, B도 connectionCount++로 11로 저장. 실제로는 connectionCount = 12가 되어야 하는데 둘 다 11이 되어버림. > 동시성 문제 발생

    //락(rock)이란, 잠금 장치. 어떤 데이터(행/테이블)를 트랜잭션이 작업하는 동안 다른 트랜잭션이 건드리지 못하게 막는 것
    //select ... for update 구문이란,
    //  - 일반적인 select는 데이터를 읽기만 한다. 다른 트랜잭션도 동시에 같은 데이터를 읽고 수정할 수 있음
    //  - select .. for update는 데이터를 읽으면서 쓰기 잠금(exclusive lock)을 건다.
    //          : 내가 이 데이터를 읽고 수정할 거니까 다른 사람은 나 끝날 때까지 건들지마! 라는 의미)
    //          : 그래서 다른 트랜잭션이 같은 행을 수정하려 하면 대기(blocking)하게 된다
    @Lock(LockModeType.PESSIMISTIC_WRITE) //나는 이 데이터를 읽고 곧 수정할 거야. 다른 트랜잭션은 나 끝날 때까지 이 데이터 수정하지 마
    Optional<UserEntity> findForUpdateByUserId(@NonNull Long userId);
}
