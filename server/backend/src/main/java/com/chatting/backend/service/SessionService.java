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
 * - 로그인 세션과 Redis를 활용한 "현재 활성 채널" 관리
 *
 * 하는 일:
 * 1) 세션 TTL 연장 (KeepAlive)
 * 2) Redis에 현재 활성 채널 저장/삭제
 * 3) 여러 사용자 중 "특정 채널을 실제로 보고 있는 사용자"만 추려내기
 *
 * 카카오톡 비유:
 * - "나 아직 접속 중이에요" → TTL 갱신
 * - "내가 지금 A방 보고 있어요" → Redis에 기록
 * - "이 단체방 안에 지금 실제로 보고 있는 사람 누구?" → Redis 조회
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



    /**
     * 여러 명의 사용자 중에서,
     * 지금 특정 채널(channelId)을 실제로 열어보고 있는 사용자(UserId)만 선별하여 반환하는 메서드
     *
     *  동작 원리
     * - DB에는 "채널 참여자"가 모두 기록되어 있지만,
     *   실제로 지금 채팅방을 열어두고 있는지 여부는 Redis에 저장된 값으로 확인한다.
     *
     * - Redis에 저장되는 값의 구조:
     *   key   = "message:user:{userId}:channel_id"
     *   value = 현재 사용자가 열어둔 채널 ID (문자열)
     *
     * 예시:
     *   DB 참여자: [U1, U2, U3]
     *   Redis 값:
     *     - U1 → "5"   (채널 5 열람 중)
     *     - U2 → "7"   (다른 채널 열람 중)
     *     - U3 → null  (어느 채널도 열람하지 않음)
     *
     *   target channelId = "5"
     *   → 결과: [U1]   (현재 이 채널을 실제로 보고 있는 사용자)
     *
     * @param channelId 지금 확인하려는 채널 ID
     * @param userIds   이 채널의 참여자 목록 (DB에서 가져온 값)
     * @return          실제로 이 채널을 열어보고 있는 사용자 목록
     */
    public List<UserId> getOnlineParticipantUserIds(ChannelId channelId, List<UserId> userIds){
        // 1) 각 userId → Redis 키 문자열로 변환 (예: "message:user:12345:channel_id")
        //    buildChannelIdKey(UserId)는 "현재 사용자가 보고 있는 채널을 기록한 Redis 키"를 만들어 주는 유틸입.
        //    ※ 이 키에 들어있는 값(value)이 바로 "그 유저가 지금 보고 있는 채널 id"이다.
        List<String> channelIdKeys = userIds.stream().map(this::buildChannelIdKey).toList(); //redis key값을 저장(key는 유저id, value는 채널id 이다)

        try{
            // 2) redis MGET: 여러 키의 값을 한 번에 가져온다(네트워크 왕복 1회).
            //    multiGet의 반환 리스트는 "요청한 keys와 동일한 순서"를 가진다.
            //    각 원소는 해당 user의 "현재 활성 채널 id 문자열" 또는 null(키 없음/TTL만료) 이다.
            List<String> channelIds = stringRedisTemplate.opsForValue().multiGet(channelIdKeys); //위에서 저장한 key에 대응되는 value값(채널id)을 저장

            if (channelIds != null) {
                // 결과 리스트(반환값). 크기를 미리 userIds.size()로 잡아 재할당 비용을 줄인다.
                List<UserId> onlineParticipantUserIds = new ArrayList<>(channelIds.size()); //현재 채널화면을 보고 있는 userId를 저장하기 위해 channelIds의 사이즈만큼의 자리를 만듦

                // 비교 대상 채널 id를 문자열로 준비 (Redis에서 꺼낸 값도 문자열이므로 문자열 비교가 필요)
                String chId = channelId.id().toString(); // 문자열로 변환

                // 3) 같은 인덱스를 가진 값끼리 비교
                //    - i번째 channelIds.get(i) 는 i번째 userIds.get(i)가 현재 보고 있는 채널 id(문자열)이다.
                //    - value가 null이면 해당 유저에 대한 키가 없거나 TTL 만료 → 현재 어떤 채널도 기록되어 있지 않다고 간주
                for(int idx = 0; idx < userIds.size(); idx++){ // for문으로 userIds(그룹채팅 참여자들)을 돌면서 해당 채널을 보고 있으면 위에서 만든 onlineParticipantUserIds에 넣기
                    String value = channelIds.get(idx);// i번째 유저의 "현재 활성 채널 id(문자열)" 또는 null

                    // 핵심 라인: 온라인이면 그 userId를, 아니면 null을 결과에 추가한다.
                    //    - value != null          : Redis에 값이 존재(어떤 채널을 보고 있음)
                    //    - value.equals(chId)     : 지금 확인하는 channelId와 동일한 채널을 보고 있음
                    //    - ? userIds.get(idx) : null  → 일치하면 해당 userId, 아니면 null
                    onlineParticipantUserIds.add(value != null && value.equals(chId) ? userIds.get(idx) : null);

                }

                // !!! 주의: 이 메서드는 "온라인만 담긴 압축 리스트"가 아니라
                //          "원래 순서를 유지하되 오프라인은 null" 인 리스트를 반환한다.
                //          (호출 측에서 null을 필터링해야 '온라인만'의 리스트가 된다.)
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
