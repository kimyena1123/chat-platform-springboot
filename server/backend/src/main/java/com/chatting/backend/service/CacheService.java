package com.chatting.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * =====================================================================
 * CacheService
 * ---------------------------------------------------------------------
 * Redis(인메모리 캐시 서버)에서 데이터를 쉽게 읽고/쓰고/삭제할 수 있도록
 * Spring의 StringRedisTemplate을 이용해 "캐시 접근 로직"을 캡슐화한 서비스 클래스.
 *
 * [주요 기능]
 *  - get() / multiGet(): 캐시에서 값 조회
 *  - set(): 캐시에 값 저장 (TTL 설정 가능)
 *  - delete(): 캐시 삭제 (단건/다건)
 *  - buildKey(): Redis 키를 규칙적으로 생성하기 위한 유틸리티
 *
 * 이 클래스는 Controller나 Service 계층에서 Redis를 직접 다루지 않고
 * "의미 있는 메서드 이름"으로 간단하게 사용할 수 있게 도와준다.
 * =====================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    // Spring에서 제공하는 Redis 문자열 전용 템플릿
    //  → "key-value" 형태로 문자열 데이터를 Redis에 쉽게 저장하고 조회할 수 있다.
    private final StringRedisTemplate stringRedisTemplate;



    // ----------------------------------------------------------------------
    //  1. 단일 key 조회
    // ----------------------------------------------------------------------
    public Optional<String> get(String key) {
        try {
            // Redis에서 해당 key의 value를 조회
            String value = stringRedisTemplate.opsForValue().get(key);

            if (value != null) {
                return Optional.of(value);
            }
        } catch (Exception ex) {
            log.error("Redis get failed. key: {}, cause: {}", key, ex.getMessage());
        }

        return Optional.empty();  // 조회 실패 또는 null일 경우 빈 Optional 반환
    }


    // ----------------------------------------------------------------------
    //  2. 여러 key를 한 번에 조회 (성능 최적화용)
    // ----------------------------------------------------------------------
    public List<String> get(Collection<String> keys) {
        try {
            // 여러 키를 한 번에 조회할 때는 multiGet()을 사용하면
            // Redis 네트워크 호출 횟수를 줄여 성능이 향상된다.
            return stringRedisTemplate.opsForValue().multiGet(keys);

        } catch (Exception ex) {
            log.error("Redis mget failed. keys: {}, cause: {}", keys, ex.getMessage());
        }

        return Collections.emptyList(); // 실패 시 빈 리스트 반환
    }


    // ----------------------------------------------------------------------
    //  3. 캐시에 값 저장
    // ----------------------------------------------------------------------
    public boolean set(String key, String value, Long ttlSeconds) {
        try {
            // opsForValue().set(키, 값, TTL, 단위)
            stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            return true;

        } catch (Exception ex) {
            log.error("Redis set failed. key: {}, cause: {}", key, ex.getMessage());
        }

        return false;
    }


    // ----------------------------------------------------------------------
    //  4. Redis의 TTL 갱신
    // ----------------------------------------------------------------------
    public boolean expire(String key, Long ttlSeconds){
        try{
            stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            return true;
        }catch (Exception ex){
            log.error("Redis expire failed. key: {}, cause: {}", key, ex.getMessage());
        }
        return false;
    }


    // ----------------------------------------------------------------------
    //   5. 단일 key 삭제
    // ----------------------------------------------------------------------
    public boolean delete(String key) {
        try {
            stringRedisTemplate.delete(key);
            return true;

        } catch (Exception ex) {
            log.error("Redis delete failed. key: {}, cause: {}", key, ex.getMessage());
        }

        return false; // delete 실패
    }


    // ----------------------------------------------------------------------
    //   6. 여러 key 삭제 (성능 개선)
    // ----------------------------------------------------------------------
    public boolean delete(Collection<String> keys) {
        try {
            stringRedisTemplate.delete(keys);
            return true;
        } catch (Exception ex) {
            log.error("Redis multi delete failed. keys: {}, cause: {}", keys, ex.getMessage());
        }
        return false;
    }



    // ----------------------------------------------------------------------
    //   7. Redis Key 생성 유틸 (1) — 단일 키 버전
    // ----------------------------------------------------------------------
    /**
     * Redis에 저장될 "고유한 키"를 만드는 함수.
     * 예시:
     *   prefix = "message:user"
     *   key = "123"
     *   → 결과: "message:user:123"
     *
     * [언제 사용하나]
     * - 단일 식별자로 캐시를 관리할 때 사용.
     *   예: 한 사용자에 대한 캐시, 한 채널에 대한 캐시 등.
     *
     * @param prefix  RedisKeyPrefix 클래스에 정의된 키 prefix (네임스페이스)
     * @param key     실제 식별자 (예: userId, channelId 등)
     */
    public String buildKey(String prefix, String key) {
        // "%s:%s" → prefix와 key를 콜론(:)으로 연결
        return "%s:%s".formatted(prefix, key);
    }


    // ----------------------------------------------------------------------
    //   8. Redis Key 생성 유틸 (2) — 2단 키 버전
    // ----------------------------------------------------------------------
    /**
     * Redis 키를 3단 구조로 만들 때 사용.
     *
     * 예시:
     *   prefix = "message:connection:status"
     *   firstKey = "userId_1"
     *   secondKey = "userId_2"
     *
     *   → 결과: "message:connection:status:userId_1:userId_2"
     *
     * [왜 필요한가]
     * - 채팅 프로젝트에서는 "나와 상대방 간의 연결 상태" 같은
     *   **2명 관계 데이터**를 캐시에 저장할 일이 많기 때문.
     * - 예를 들어, A(1번)와 B(2번)의 연결 상태를 저장하려면
     *   Redis에 다음과 같은 키가 생긴다:
     *      message:connection:status:1:2
     *
     * [파라미터 의미]
     *  - prefix: Red isKeyPrefix.CONNECTION_STATUS 와 같은 상수 (예: "message:connection:status")
     *  - firstKey: 첫 번째 주체 (예: A의 userId)
     *  - secondKey: 두 번째 주체 (예: B의 userId)
     */
    public String buildKey(String prefix, String firstKey, String secondKey) {
        // "%s:%s:%s" → prefix + 첫 키 + 두 번째 키
        return "%s:%s:%s".formatted(prefix, firstKey, secondKey);
    }
}
