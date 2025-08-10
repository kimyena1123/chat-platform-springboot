package com.chatting.backend.service;

import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.entity.UserEntity;
import com.chatting.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 및 회원탈퇴 기능
 * SessionService를 통해 현재 로그인한 사용자 확인 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    //현재 세션에서 사용자 이름(username)을 가져오거나 TTL 갱신하는 서비스
    private final SessionService sessionService;

    //사용자 정보를 CRUD할 수 있는 JPA repository
    private final UserRepository userRepository;

    //비밀번호를 암호화할 때 사용하는 Spring Security 제공 인터페이스
    //주의: @RequiredArgsConstructor를 사용하려면 final 키워드를 붙여야 자동 생성됨
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자를 등록하는 메서드
     * @param username 사용자 아이디
     * @param password 비밀번호 (암호화 전)
     * @return 생성된 사용자의 ID (UserId 객체로 감쌈)
     */
    @Transactional //트랜잭션 처리: 이 메서드가 실패하면 콜백됨
    public UserId addUser(String username, String password) {
        //1. 비밀번호를 암호화한 후, 새로운 사용자 엔티티 생성
        UserEntity messageUserEntity = new UserEntity(username, passwordEncoder.encode(password));

        //2. 상요자 정보를 DB에 저장
        messageUserEntity = userRepository.save(messageUserEntity);

        // 3. 로그 출력 (등록 성공)
        log.info("User registered. UserId: {}, username: {}",messageUserEntity.getUserId(), messageUserEntity.getUsername());

        // 4. 사용자 ID만을 감싸서 리턴
        return new UserId(messageUserEntity.getUserId());
    }

    /**
     * 현재 로그인한 사용자를 삭제하는 메서드
     * 1. 세션에서 username을 가져온다
     * 2. username으로 DB에서 해당 사용자 정보를 찾는다
     * 3. 사용자 정보를 DB에서 삭제한다
     */
    @Transactional // 이 작업도 트랜잭션으로 처리
    public void removeUser() {
        // 1. 현재 로그인한 사용자의 이름을 세션에서 가져온다
        String username = sessionService.getUsername();

        // 2. 해당 사용자를 데이터베이스에서 조회 (없으면 예외 발생)
        UserEntity messageUserEntity = userRepository.findByUsername(username).orElseThrow(); // 예외 메시지를 커스텀할 수도 있음

        // 3. 해당 사용자 ID로 사용자 삭제
        userRepository.deleteById(messageUserEntity.getUserId());

        // 4. 로그 출력 (삭제 성공)
        log.info("User deleted. UserId: {}, username: {}", messageUserEntity.getUserId(), messageUserEntity.getUsername());
    }
}
