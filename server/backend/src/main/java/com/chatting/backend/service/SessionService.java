package com.chatting.backend.service;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.domain.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * [SessionService]
 * - 로그인 세션 유지와 사용자의 "현재 활성 채널" 정보를 관리하는 서비스
 * - Spring Session + Redis를 활용
 *
 * 주요 기능:
 * 1) 로그인 세션 TTL(유효시간) 연장 → 사용자가 끊기지 않고 계속 접속할 수 있도록 보장
 * 2) 사용자가 현재 들어가 있는 채널(channel)을 Redis에 기록 → 실시간 알림/읽음 처리 등에 활용
 * 3) 사용자가 특정 채널에 "현재 온라인 상태인지" 확인 (isOnline)
 *
 * 카카오톡 비유:
 * - TTL 연장: 카톡 앱이 "나 아직 살아있어요" 핑을 보내면 로그아웃되지 않고 유지
 * - setActiveChannel: 지금 어떤 방을 보고 있는지 서버에 기록 (A방인지 B방인지)
 * - isOnline: 특정 방에 이 사람이 현재 접속해 있는지 확인 (푸시 알림 줄지 말지 판단 가능)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {


    // Spring Session이 제공하는 세션 저장소
    // httpSessionId로 세션을 찾아서 "마지막 접근 시간(lastAccessedTime)"을 갱신하는 데 사용
    private final SessionRepository<? extends Session> httpSessionRepository;

    // Redis에 문자열 기반 데이터 저장/조회용 템플릿(Redis 접근용 템플릿)
    private final StringRedisTemplate stringRedisTemplate;

    // TTL 설정: Redis 키의 유효시간 (초 단위, 여기서는 300초 = 5분). 이 시간동안만 "활성 채널" 키가 유지되고, 이후 작동으로 삭제
    // KeepAlive로 주기적으로 연장해 주면 계속 살아있다
    private final long TTL = 300;


    /**
     * [현재 로그인한 사용자 이름 가져오기]
     * - Spring Security의 SecurityContext에서 Authentication 객체를 꺼내 username 반환
     */
    public String getUsername() {
        //현재 연결되어 있는 세션, 내 세션에서 내 이름(username)이 필요함 -> security의 도움을 받을 수 있음
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return username;
    }


    /**
     * [세션 TTL 연장] : 전닯받은 httpSessionId를 이용해 세션을 찾고, 해당 세션이 존재한다면 마지막 접근 시간을 현재 시각으로 갱신한다(TTL 초기화 효과)
     * - HTTP 세션 TTL과 Redis에 저장된 "활성 채널 키" TTL을 같이 연장한다 : (HTTP 세션 TTL(유효 시간)을 연장하고, Redis의 "활성 채널" 키 TTL도 함께 연장한다.)
     *      - 보통 WebSocket KeepAlive 요청 시 호출됨.
     *
     * @param userId 현재 사용자 ID
     * @param httpSessionId 브라우저의 HTTP 세션 ID
     */
    public void refreshTTL(UserId userId, String httpSessionId) {
        // Redis에서 사용할 활성 채널 키를 구성. (예: message:user:12345:channel_id)
        String channelIdKey = buildChannelIdKey(userId);
        log.info("##### SessionService > refreshTTL method; channelKey: {} #####", channelIdKey);

        try {
            // 1) HTTP 세션을 세션 저장소에서 찾고
            Session httpSession = httpSessionRepository.findById(httpSessionId);

            // 2) 세션이 존재하면 "마지막 접근 시각"을 현재 시각으로 갱신 → Spring Session이 TTL을 연장함(TTL 초기화)
            if (httpSession != null) {
                httpSession.setLastAccessedTime(Instant.now()); // 세션 타임아웃 시간 계산의 기준점 업데이트

                // 3) Redis의 활성 채널 키도 TTL을 함께 연장 (둘을 동일한 주기로 묶어 유지)
                //      - Redis에서도 해당 키 TTL 연장
                stringRedisTemplate.expire(channelIdKey, TTL, TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            // expire 실패 시에도 서비스 전체가 죽을 필요는 없으므로 로깅만 합니다.
            log.error("Redis expire failed. key: {}", channelIdKey);
        }

        //만약 세션이 존재하지 않으면 아무 작업도 하지 않음(예. 세션이 만료됐거나 잘못된 ID)
    }


    /** [Redis 키 생성 유틸]
     * - userId와 channel_id를 묶어 Redis 키를 만든다.
     * - 예시: "message:user:12345:channel_id"
     *
     * @param userId 현재 사용자 식별자
     * @return "message:user:{userId}:{channel_id}" 형태의 키 문자열
     */
    private String buildChannelIdKey(UserId userId) {

        String NAMESPACE = "message:user";
        return "%s:%d:%s".formatted(NAMESPACE, userId, IdKey.CHANNEL_ID);
    }


    /** [현재 활성 채널 기록] : 사용자의 "현재 활성 채널"을 Redis에 기록 (값 = channelId, TTL = 5분)
     *
     * 언제 호출?
     *   - 사용자가 특정 채널(대화방)에 "입장"할 때 ChannelService.enter()에서 호출힌다.
     *   - 이후 KeepAlive가 올 때마다 refreshTTL()에서 expire로 TTL을 연장한둔.
     * 카카오톡 비유
     *   - 유저가 A와의 대화방을 열면, 서버에 "OOO님은 지금 A방을 보고 있어요"라고 적어둔다.
     *   - 새 메시지가 왔을 때 "이미 그 방을 보고 있으면 별도 알림 뱃지를 안 붙인다" 같은 로직을 만들 수 있다.
     *
     * @param userId    사용자 식별자
     * @param channelId 현재 들어간 채널 식별자
     * @return Redis set 성공 여부
     */
    public boolean setActiveChannel(UserId userId, ChannelId channelId) {
        //키 만들기
        String channelIdKey = buildChannelIdKey(userId);

        try {
            stringRedisTemplate.opsForValue().set(channelIdKey, channelId.id().toString(), TTL, TimeUnit.SECONDS);
            return true;
        } catch (Exception ex) {
            log.error("Redis set failed. key: {}, channelId: {}", channelIdKey, channelId);
            return false;
        }
    }


    /**
     * [사용자가 특정 채널에 온라인 상태인지 확인]
     * - Redis에서 "userId의 활성 채널" 값을 가져와, 지금 보고 있는 채널과 같은지 확인한다.
     *
     * 활용 예시 (카카오톡 비유):
     * - 친구 A가 B방에 메시지를 보냄 → 서버가 Redis를 확인
     * - "B가 지금 그 방을 보고 있다면 → 푸시 알림 X, 바로 읽음 처리"
     * - "B가 그 방을 보고 있지 않다면 → 푸시 알림 O"
     *
     * @param userId 사용자 ID
     * @param channelId 확인할 채널 ID
     * @return true = 해당 채널에 온라인 상태, false = 오프라인/다른 채널에 있음
     */
    //참여자가 활동중인 채널 = 사용자가 메시지를 보내려는 채널인지 확인
    public boolean isOnline(UserId userId, ChannelId channelId) {
        String channelIdKey = buildChannelIdKey(userId);
        try {
            String chId = stringRedisTemplate.opsForValue().get(channelIdKey);

            if (chId != null && chId.equals(channelId.id().toString())) {
                return true;
            }

        } catch (Exception ex) {
            log.error("Redis get failed. key: {}, cause: {}", channelIdKey, ex.getMessage());
        }
        return false; // 온라인이 아니면 false
    }

}
