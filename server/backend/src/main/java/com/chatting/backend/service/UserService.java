package com.chatting.backend.service;

import com.chatting.backend.constant.RedisKeyPrefix;
import com.chatting.backend.dto.domain.InviteCode;
import com.chatting.backend.dto.domain.User;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.projection.UsernameProjection;
import com.chatting.backend.entity.UserEntity;
import com.chatting.backend.json.JsonUtil;
import com.chatting.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * =====================================================================
 * [UserService]
 * ---------------------------------------------------------------------
 * 사용자 정보(username, userId, inviteCode 등)를 관리하는 서비스.
 *
 * 주요 특징:
 *  - Redis 캐시와 DB(MySQL)를 함께 사용.
 *  - 자주 조회되고 잘 변하지 않는 데이터(username 등)는 캐시 저장.
 *  - 자주 변하는 데이터(list 등)는 캐시 저장 X (정렬/인코딩 비용이 큼).
 *
 * [캐시 로직 흐름]
 *  1. Redis에서 key로 value 조회
 *  2. 있으면 그대로 반환
 *  3. 없으면 DB 조회 → 캐시에 set() 후 반환
 *
 * [JSON 직렬화 사용 규칙]
 *  - 단일 값(String, Long) → 그냥 저장
 *  - 객체(User)나 List<User> → JSON으로 변환 후 저장
 * =====================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SessionService sessionService;
    private final CacheService cacheService;
    private final UserRepository userRepository;
    private final JsonUtil jsonUtil;

    // 비밀번호를 암호화할 때 사용하는 Spring Security 제공 인터페이스
    // 주의: @RequiredArgsConstructor를 사용하려면 final 키워드를 붙여야 자동 생성됨
    private final PasswordEncoder passwordEncoder;

    // 현재 채팅 서비스에서 username은 못바꾼다.
    // 사용자에게 username을 바꿀 수 있는 기능을 안만들었기 때문에 한번 유저가 등록되면 안바뀌는 값이라서 캐시에 오래 있어도 괜찮다.
    private final long TTL = 3600;  // 캐시 TTL: 1시간


    /**
     * userId로 username 정보 가져오기
     */
    @Transactional(readOnly = true) //DB 조작은 없고, DB 조회한다
    public Optional<String> getUsername(UserId userId){

        // Redis(캐시)에서 key 값을 가져온다
        String key = cacheService.buildKey(RedisKeyPrefix.USERNAME, userId.id().toString());

        //Redis에서 해당 key에 대한 value를 조회해 가져온다
        Optional<String> cachedUsername = cacheService.get(key);

        // value가 존재하면 그대로 리턴하고,
        // 없으면 Db에서 조회한 후 Redis에 set한다.
        if(cachedUsername.isPresent()){
            return cachedUsername;
        }

        Optional<String> fromDB = userRepository.findByUserId(userId.id()).map(UsernameProjection::getUsername);
        fromDB.ifPresent(username -> cacheService.set(key, username, TTL));

        return fromDB;
    }

    /**
     * username으로 userId를 찾는 메서드
     */
    @Transactional(readOnly = true) //DB 조작은 없고, DB 조회한다
    public Optional<UserId> getUserId(String username){
        String key = cacheService.buildKey(RedisKeyPrefix.USER_ID, username);
        Optional<String> cachedUserId = cacheService.get(key);
        if(cachedUserId.isPresent()){
            return Optional.of(new UserId(Long.valueOf(cachedUserId.get())));
        }

        Optional<UserId> fromDB = userRepository.findUserIdByUsername(username).map(userIdProjection -> new UserId(userIdProjection.getUserId()));
        fromDB.ifPresent(userId -> cacheService.set(key, userId.id().toString(), TTL));

        return fromDB;
    }


    /**
     * 여러 username 값으로 userId 목록 조회하기
     *  - 리스트 정렬/인코딩 비용이 크므로 캐시 사용 X (자주 호출된다면 나중에 Redis 사용)
     */
    //여기는 cache처리 안한다.
    @Transactional(readOnly = true) //DB 조작은 없고, DB 조회한다
    public List<UserId> getUserIds(List<String> usernames){
        //해당하는 여러 개의 userId가 나옴
        return userRepository.findByUsernameIn(usernames).stream().map(userIdProjection -> new UserId(userIdProjection.getUserId())).toList();
    }


    /**
     * 초대코드로 username을 찾는 메서드
     * - User는 복합 객체 → JSON 직렬화로 캐시에 저장.
     */
    @Transactional(readOnly = true) //DB 조작은 없고, DB 조회한다
    public Optional<User> getUser(InviteCode inviteCode){
        String key = cacheService.buildKey(RedisKeyPrefix.USER, inviteCode.code());
        Optional<String> cachedUser = cacheService.get(key);
        if(cachedUser.isPresent()){
            // JSON → User 객체로 역직렬화
            return jsonUtil.fromJson(cachedUser.get(), User.class);
        }

        Optional<User> fromDB = userRepository.findByInviteCode(inviteCode.code()).map(userEntity -> new User(new UserId(userEntity.getUserId()), userEntity.getUsername()));
        // DB에서 찾은 User를 JSON으로 변환 후 Redis 저장
        fromDB.flatMap(jsonUtil::toJson).ifPresent(json -> cacheService.set(key, json, TTL));

        return fromDB;
    }


    /**
     * [userId → inviteCode 조회]
     * - 단일 문자열 값이므로 JSON 변환 불필요.
     */
    public Optional<InviteCode> getInviteCode(UserId userId){
        String key = cacheService.buildKey(RedisKeyPrefix.USER_INVITECODE, userId.id().toString());
        Optional<String> cachedUserInviteCode = cacheService.get(key);

        if (cachedUserInviteCode.isPresent()) {
            return Optional.of(new InviteCode(cachedUserInviteCode.get()));
        }

        Optional<InviteCode> fromDB = userRepository.findInviteCodeByUserId(userId.id()).map(inviteCodeProjection -> new InviteCode(inviteCodeProjection.getInviteCode()));
        fromDB.ifPresent(inviteCode -> cacheService.set(key, inviteCode.code(), TTL));

        return fromDB;
    }


    /**
     * [userId → 연결된 친구 수 조회]
     * - 값이 자주 바뀔 수 있으므로 캐시 사용 X.
     */
    @Transactional(readOnly = true) //DB 조작은 없고, DB 조회한다
    public Optional<Integer> getConnectionCount(UserId userId){
        return userRepository.findCountByUserId(userId.id()).map(countProjection -> countProjection.getConnectionCount());
    }


    /**
     * 사용자를 등록하는 메서드
     * - 새로운 유저를 DB에 저장.
     * - 캐시 등록은 안 함 (처음 등록이므로 캐시에 없음).
     *
     * @param username 사용자 아이디
     * @param password 비밀번호 (암호화 전)
     * @return 생성된 사용자의 ID (UserId 객체로 감쌈)
     */
    @Transactional // DB를 조작하는거라 사용
    public UserId addUser(String username, String password) {
        //1. 비밀번호를 암호화한 후, 새로운 사용자 엔티티 생성
        UserEntity messageUserEntity = new UserEntity(username, passwordEncoder.encode(password));

        //2. 사용자 정보를 DB에 저장
        messageUserEntity = userRepository.save(messageUserEntity);

        // 3. 로그 출력 (등록 성공)
        log.info("User registered. UserId: {}, username: {}",messageUserEntity.getUserId(), messageUserEntity.getUsername());

        // 4. 사용자 ID만을 감싸서 리턴
        return new UserId(messageUserEntity.getUserId());
    }

    /**
     * 현재 로그인한 사용자를 삭제하는 메서드
     *  - DB에서 삭제 후 Redis 캐시도 같이 삭제.
     *  - delete() 사용 → 캐시에서 완전 제거.
     *
     *  addUser는 새로 등록되는 것이기에 애초이 cache(Redis)에 없다.
     *  removeUser는 이미 캐시에 올라가져 있을 수 있기에 사용자 삭제할 때 Redis에서도 삭제해줘야 한다.
     */
    @Transactional // DB를 조작하는거라 사용
    public void removeUser() {
        String username = sessionService.getUsername();

        UserEntity userEntity = userRepository.findByUsername(username).orElseThrow();
        String userId = userEntity.getUserId().toString();

        userRepository.deleteById(userEntity.getUserId());
        cacheService.delete(
                List.of(
                        cacheService.buildKey(RedisKeyPrefix.USER_ID, username),
                        cacheService.buildKey(RedisKeyPrefix.USERNAME, userId),
                        cacheService.buildKey(RedisKeyPrefix.USER, userId),
                        cacheService.buildKey(RedisKeyPrefix.USER_INVITECODE, userId)
                )
        );

        log.info("User unregistered. UserId: {}, Username: {}", userEntity.getUserId(), userEntity.getUsername());

    }
}
