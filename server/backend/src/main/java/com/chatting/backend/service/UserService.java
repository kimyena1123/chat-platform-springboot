package com.chatting.backend.service;

import com.chatting.backend.dto.domain.InviteCode;
import com.chatting.backend.dto.domain.User;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.projection.UsernameProjection;
import com.chatting.backend.entity.UserEntity;
import com.chatting.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
     * userId로 username 정보 가져오기
     */
    public Optional<String> getUsername(UserId userId){
        //map()을 쓰는 이유는 findByUserId()의 반환 타입과 getUsername()의 반환 타입이 다르기 때문
        //map: Optional 안에 값이 있으면 그 값을 변환하고, 없으면 그대로 빈 Optional을 리턴한다.
        return userRepository.findByUserId(userId.id())
                .map(UsernameProjection::getUsername);
    }

    /**
     * username으로 userId를 찾는 메서드
     */
    public Optional<UserId> getUserId(String username){
        return userRepository.findByUsername(username).map(userEntity -> new UserId(userEntity.getUserId()));
    }

    /**
     * 여러 username 값으로 userId 목록 조회하기
     */
    public List<UserId> getUserIds(List<String> usernames){
        //해당하는 여러 개의 userId가 나옴
        return userRepository.findByUsernameIn(usernames).stream().map(userIdProjection -> new UserId(userIdProjection.getUserId())).toList();
    }

    /**
     * 초대코드로 username을 찾는 메서드
     */
    public Optional<User> getUser(InviteCode inviteCode){
        return userRepository.findByConnectionInviteCode(inviteCode.code())
                .map(entity -> new User(new UserId(entity.getUserId()), entity.getUsername()));
    }

    /**
     * userId로 inviteCode 찾는 메서드
     */
    public Optional<InviteCode> getInviteCode(UserId userId){
        return userRepository.findInviteCodeByUserId(userId.id()).map(inviteCodeProjection -> new InviteCode(inviteCodeProjection.getConnectionInviteCode()));
    }

    /**
     * userId로 count를 찾는 메서드 (해당 userId를 가진 사용자의 연결수 찾기)
     */
    public Optional<Integer> getConnectionCount(UserId userId){
        return userRepository.findCountByUserId(userId.id()).map(countProjection -> countProjection.getConnectionCount());
    }

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
