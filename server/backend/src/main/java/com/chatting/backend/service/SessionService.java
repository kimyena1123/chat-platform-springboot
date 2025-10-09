package com.chatting.backend.service;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.RedisKeyPrefix;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * [SessionService]
 * - 로그인 세션(Spring Session) + Redis 를 함께 써서
 *   "유저가 지금 어떤 채널 화면을 실제로 보고 있는가(Active Channel)"를 관리하는 서비스.
 *
 * 주요 책임
 *  1) 세션 TTL 갱신(keep-alive): 웹소켓/폴링 등 주기 요청이 들어오면 HTTP 세션 TTL을 연장
 *  2) Redis에 "현재 활성 채널" 기록/삭제: 유저가 채널 입장/이탈 시 상태를 캐시에 반영
 *  3) 채널 참여자 중에서 "지금 이 채널을 열어둔 사용자들"만 빠르게 선별
 *
 * 저장 포맷(예시)
 *  - 키   : message:user:{userId}:channel_id
 *  - 값   : {channelId} (문자열)
 *  - TTL  : 300초 (5분) — keep-alive 요청이 오면 함께 연장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    // Spring Session 저장소: httpSessionId 로 세션을 찾아서
    // 마지막 접근 시각(lastAccessedTime) 갱신 → 세션 TTL 연장에 사용
    private final SessionRepository<? extends Session> httpSessionRepository;

    // Redis 접근을 캡슐화한 유틸 서비스 (get/set/delete/expire 등)
    private final CacheService cacheService;

    // Redis 키의 기본 생존 시간(초). 여기서는 5분.
    // - setActiveChannel 시 부여
    // - refreshTTL 시 연장
    private final long TTL = 300;


    /**
     * [현재 로그인한 사용자 이름 가져오기]
     * - Spring Security의 SecurityContext에서 Authentication 객체를 꺼내 username 반환
     */
    public String getUsername() {
        //현재 연결되어 있는 세션, 내 세션에서 내 이름(username)이 필요함 -> security의 도움을 받을 수 있음
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }


    /**
     * [지금 특정 채널 화면을 열어보고 있는 유저만 선별]
     *
     * 배경:
     *  - DB에는 "채널 참여자 전체"가 들어있지만,
     *    진짜로 현재 화면을 열어두었는지는 Redis에만 즉시성 있게 저장해 둔다.
     *
     * 동작:
     *  1) 전달받은 참여자 userIds 에 대해,
     *     각 유저의 "활성 채널 키(message:user:{uid}:channel_id)"를 만든다.
     *  2) 멀티 조회(mGet)로 각 키의 값을 한 번에 가져온다(각 유저가 열어둔 채널 ID).
     *  3) 가져온 값이 내가 찾는 channelId 와 같으면 "실제 열람 중"으로 간주하여 목록에 추가.
     *
     * 반환:
     *  - 실제로 channelId 를 열어보고 있는 사용자(UserId)만 담은 리스트
     */
    public List<UserId> getOnlineParticipantUserIds(ChannelId channelId, List<UserId> userIds) {
        // 각 유저의 "활성 채널" 레디스 키 리스트 생성
        List<String> channelIdKeys = userIds.stream().map(this::buildChannelIdKey).toList();

        // mGet: 여러 키를 한 번에 조회(성능상 유리)
        List<String> channelIds = cacheService.get(channelIdKeys);


        if (channelIds != null) {
            List<UserId> onlineParticipantUserIds = new ArrayList<>(userIds.size());
            String chId = channelId.id().toString();

            // mGet 결과(channelIds)의 인덱스와 userIds의 인덱스가 1:1로 대응
            for (int idx = 0; idx < userIds.size(); idx++) {
                String value = channelIds.get(idx); // i번째 유저가 열어둔 채널 ID (없으면 null)

                // 값이 있고, 내가 찾는 채널과 같으면 "현재 열람 중"으로 간주
                onlineParticipantUserIds.add(value != null && value.equals(chId) ? userIds.get(idx) : null);
            }
            return onlineParticipantUserIds;
        }
        return Collections.emptyList();
    }


    /** [현재 활성 채널 기록] : 사용자의 "현재 활성 채널"을 Redis에 기록 (값 = channelId, TTL = 5분)
     *
     * 언제 호출?
     *   - 사용자가 특정 채널(대화방)에 "입장"할 때 ChannelService.enter()에서 호출힌다.
     *   - 이후 KeepAlive가 올 때마다 refreshTTL()에서 expire로 TTL을 연장.
     * @param userId    사용자 식별자
     * @param channelId 현재 들어간 채널 식별자
     * @return Redis set 성공 여부
     */
    public boolean setActiveChannel(UserId userId, ChannelId channelId) {
        return cacheService.set(buildChannelIdKey(userId), channelId.id().toString(), TTL);
    }


    /**
     * [활성 채널 제거] — 유저가 채널에서 나가거나, 어떤 채널도 보고 있지 않을 때 호출.
     * - 사용자의 활성 채널은 항상 "최대 1개"라는 가정이므로 channelId 파라미터는 불필요.
     * - Redis에서 해당 유저의 활성 채널 키 자체를 삭제.
     */
    public boolean removeActiveChannel(UserId userId) {
        return cacheService.delete(buildChannelIdKey(userId));
    }


    /**
     * [세션 + 활성 채널 TTL 동시 연장]
     * - 웹소켓 ping/keep-alive 요청 같은 주기 호출에서 사용.
     *
     * 흐름:
     *  1) httpSessionId 로 Spring Session을 조회
     *  2) 세션이 존재하면 lastAccessedTime 을 현재 시각으로 갱신 → 세션 TTL 연장
     *  3) 동일 타이밍에 Redis 측 "활성 채널 키"의 TTL 도 함께 연장
     *
     * 의도:
     *  - "브라우저가 살아있고 실제로 화면을 보고 있다"는 의미로
     *    두 측(Spring Session, Redis Active Channel)의 수명을 함께 늘려 일관성 유지
     */
    public void refreshTTL(UserId userId, String httpSessionId) {
        // Redis에서 사용할 활성 채널 키를 구성. (예: message:user:12345:channel_id)
        String channelIdKey = buildChannelIdKey(userId);

        try {
            // 1) HTTP 세션을 세션 저장소에서 찾고
            Session httpSession = httpSessionRepository.findById(httpSessionId);

            // 2) 세션이 존재하면 "마지막 접근 시각"을 현재 시각으로 갱신 → Spring Session이 TTL을 연장함(TTL 초기화)
            if (httpSession != null) {
                httpSession.setLastAccessedTime(Instant.now());

                // 3) Redis의 활성 채널 키도 TTL을 함께 연장 (둘을 동일한 주기로 묶어 유지)
                //      - Redis에서도 해당 키 TTL 연장
                cacheService.expire(channelIdKey, TTL);
            }
        } catch (Exception ex) {
            // expire 실패 시에도 서비스 전체가 죽을 필요는 없으므로 로깅만 한다.
            log.error("Redis find failed. httpSessionId: {}, cause: {}", httpSessionId, ex.getMessage());
        }
    }


    /**
     * [Redis 키 생성 유틸] — "이 유저가 지금 열어본 채널"을 저장하는 키를 표준 규칙으로 생성
     *
     * 키 규칙:
     *   message:user:{userId}:channel_id
     *     - RedisKeyPrefix.USER  = "message:user"  (도메인 네임스페이스)
     *     - userId.id()          = "{userId}"      (대상 사용자)
     *     - IdKey.CHANNEL_ID     = "channel_id"    (속성명)
     *
     * 이렇게 표준화해 두면,
     *  - 어디서든 같은 규칙으로 키를 만들 수 있고(오타/충돌 방지),
     *  - Redis 내 데이터를 사람도 쉽게 읽고 추적할 수 있다.
     */
    private String buildChannelIdKey(UserId userId) {
        return cacheService.buildKey(RedisKeyPrefix.USER, userId.id().toString(), IdKey.CHANNEL_ID.getValue());
    }






}
