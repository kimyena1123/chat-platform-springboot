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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    //Redis에 등록된 걸 삭제하는 메서드
    //  - enter()할 시 setActiveChannel()를 해서 redis 등록을 했다.
    //  - leave()할 때 redis에 등록했던 걸 지워야 한다.
    //해당 사용자의 active채널은 늘 한개여서 ChannelId를 파라미터로 안받는다.
    public boolean removeActiveChannel(UserId userId) {
        //해당 유저의 redis에 등록된 channel 관련 key값을 가져온다.
        //redis애 등록된 형식 > userId : channelId
        //buildChannelIdKey(UserId)는 "현재 사용자가 보고 있는 채널을 기록한 Redis 키"를 만들어 주는 유틸입.
        String channelIdKey = buildChannelIdKey(userId);

        try{
            stringRedisTemplate.delete(channelIdKey);

            return true;
        }catch (Exception ex){
            log.error("Redis delete failed. key: {}", channelIdKey);
            return false;
        }
    }


/*
    //     * [사용자가 특정 채널에 온라인 상태인지 확인] : 특정 사용한 1명에 대한 상태 확인(단체톡방에 사용X. 개인톡방 사용O)
    //     * - Redis에서 "userId의 활성 채널" 값을 가져와, 지금 보고 있는 채널과 같은지 확인한다.
    //     *
    //     * 활용 예시 (카카오톡 비유):
    //     * - 친구 A가 B방에 메시지를 보냄 → 서버가 Redis를 확인
    //     * - "B가 지금 그 방을 보고 있다면 → 푸시 알림 X, 바로 읽음 처리"
    //     * - "B가 그 방을 보고 있지 않다면 → 푸시 알림 O"
    //     *
    //     * @param userId 사용자 ID
    //     * @param channelId 확인할 채널 ID
    //     * @return true = 해당 채널에 온라인 상태, false = 오프라인/다른 채널에 있음

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
*/



    /**
     * [현재 channelId 화면을 보고 있는 사용자만 선별해서 반환] : 추가 자세한 설명은 github commit 메시지에 넣어뒀습니다.
     *
     * userIds 목록 중에서 **지금 이 방(channelId)** 을 실제로 보고 있는(= Redis에 저장된 "활성 채널" 값이 channelId와 같은)
     * 사용자만 골라서 반환하는 **배치 조회 함수**.
     *
     * 왜 배치(멀티)로 보나?
     * - 1:1일 때는 isOnline(userId, channelId)를 한 명에게만 호출하면 됐지만,
     * - 그룹 채팅(최대 100명 등)에서는 같은 호출을 N번 반복하면 **Redis 네트워크 왕복이 N회** 발생 → 지연↑
     * - 이 메서드는 **MGET**(multiGet)으로 한 번에 N명을 조회해 **왕복 1회**로 줄이는 최적화.
     *
     * 동작 요약
     *  1) 각 userId에 대해 Redis 키 "message:user:{userId}:channel_id" 생성
     *  2) multiGet으로 모든 키의 값을 **한 번에** 조회 → 각 사용자가 현재 보고 있는 채널 id 목록
     *  3) (target) channelId 와 **값이 같은 사용자만** 선별해서 반환
     *
     * target channelId란?
     * - 이 메서드 파라미터로 들어온 `channelId`가 바로 **검사 대상 방**(지금 메시지를 보낼/보고 있어야 하는 방)이다.
     *
     * 예시(인덱스 매칭 이해용):
     *   userIds = [101, 102, 103]
     *   keys = ["message:user:101:channel_id", "message:user:102:channel_id", "message:user:103:channel_id"]
     *   Redis 값들(channelIds) = ["5", "7", "5"]   // multiGet은 요청 순서와 **동일한 순서**로 값을 반환(없으면 null)
     *   target channelId = 5라면("5"와 문자열 비교)
     *   → 101은 "5" 매칭(O), 102는 "7"(X), 103은 "5"(O)
     *   → 반환: [101, 103]
     *
     * 시간 복잡도
     * - 키 만들기 O(N) + MGET 네트워크 왕복 1회 + 선형 필터링 O(N) → 총 O(N) + 왕복 1
     */
    public List<UserId> getOnlineParticipantUserIds(ChannelId channelId, List<UserId> userIds){
        // 1) 각 userId → Redis 키 문자열로 변환 (예: "message:user:12345:channel_id")
        //    buildChannelIdKey(UserId)는 "현재 사용자가 보고 있는 채널을 기록한 Redis 키"를 만들어 주는 유틸입.
        //    ※ 이 키에 들어있는 값(value)이 바로 "그 유저가 지금 보고 있는 채널 id"이다.
        List<String> channelIdKeys = userIds.stream().map(this::buildChannelIdKey).toList(); //redis key값을 저장(key는 유저id, value는 채널id 이다)

        try{
            // 2) Redis MGET: 여러 키의 값을 한 번에 조회 (네트워크 왕복 1회)
            //    반환 리스트의 **인덱스는 요청한 키들의 인덱스와 동일**하다.
            //    값은 각 유저의 '활성 채널 id' (예: "7"). 키가 없거나 TTL 만료면 null.
            List<String> channelIds = stringRedisTemplate.opsForValue().multiGet(channelIdKeys); //위에서 저장한 key에 대응되는 value값(채널id)을 저장

            if (channelIds != null) {
                // ─────────────────────────────────────────────────────────────
                // 초기 용량 최적화: new ArrayList<>(channelIds.size())
                // ─────────────────────────────────────────────────────────────
                // 최대 userIds 수만큼 결과가 나올 수 있음을 **미리 알고** 있으므로,
                // ArrayList 내부 배열 리사이즈(재할당)를 줄이기 위해 초기 용량을 미리 잡아둔다.
                //
                // 꼭 필요하진 않지만(없어도 동작함) 그룹 인원이 많을수록 메모리 재할당 비용 절감에 유리하다.
                // 예: 단체방 100명 → 결과도 최대 100명까지 들어올 수 있음 → capacity를 100으로 선할당
                List<UserId> onlineParticipantUserIds = new ArrayList<>(channelIds.size()); //현재 채널화면을 보고 있는 userId를 저장하기 위해 channelIds의 사이즈만큼의 자리를 만듦
                String chId = channelId.id().toString(); // 문자열로 변환

                // 3) 결과를 target channelId와 비교해 **매칭되는 사용자만** 수집
                //    multiGet 결과는 요청한 keys 순서와 동일한 인덱스로 반환되므로,
                //    i번째 값은 곧 i번째 userId의 "현재 활성 채널" 값입니다.
                for(int idx = 0; idx < userIds.size(); idx++){ // for문으로 userIds(그룹채팅 참여자들)을 돌면서 해당 채널을 보고 있으면 위에서 만든 onlineParticipantUserIds에 넣기
                    String value = channelIds.get(idx); // 해당 userIds[idx]의 현재 활성 채널 (예: "5" 또는 null)

                    // value가 null이면 Redis에 키가 없거나 TTL로 만료된 상태
                    if(value != null && value.equals(chId)){
                        // 현재 이 채널을 보고 있는 사용자만 결과에 추가
                        onlineParticipantUserIds.add(userIds.get(idx));
                    }
                }
                return onlineParticipantUserIds; // online 대상만 반환
            }
        }catch (Exception ex){
            // Redis 장애/일시 오류 등: 전체 서비스가 죽을 필요 없으니 로깅 후 빈 리스트 반환
            log.error("Redis mget faild. key: {}, cause: {}", channelIdKeys, ex.getMessage());
        }
        return Collections.emptyList(); // 조회 실패/결과 없음 → 빈 리스트
    }


    /** [Redis 키 생성 유틸]
     * - 하나의 사용자에 대해 "현재 활성 채널" 값을 저장/조회할 키를 만든다.
     * - userId와 channel_id를 묶어 Redis 키를 만든다.
     * - 예시: "message:user:12345:channel_id"
     *
     * @param userId 현재 사용자 식별자
     * @return "message:user:{userId}:{channel_id}" 형태의 키 문자열
     */
    private String buildChannelIdKey(UserId userId) {

        String NAMESPACE = "message:user";
        return "%s:%d:%s".formatted(NAMESPACE, userId.id(), IdKey.CHANNEL_ID.getValue());
    }

}
