package com.chatting.backend.service;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.domain.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * [SessionService]: 로그인한 사용자의 HttpSession 정보를 기반으로 세션의 TTL(Time To Live, 유효시간)을 연장하는 기능을 제공하는 서비스 클래스이다.
 *
 * 이 클래스는 WebSocket 통신 중에도 세션 유지를 위해 TTL을 갱신해줄 필요가 있는 경우 사용된다.
 */

/**
 * ["로그인 세션 유지"와 "사용자가 현재 어느 채팅방(채널)에 머무는지"를 관리]
 * - '세션'은 카톡에 로그인해 있는 상태. 앱을 잠깐 껐다 켜도 로그인 상태가 유지되면 편하다.
 * - '현재 활성 채널(active channel)'은 지금 사용자가 보고 있는 대화방(채팅방)을 가리킨다.
 *     예) 친구 A와의 1:1 대화방에 들어가면 "OOO님의 현재 활성 채널 = A와의 대화방"으로 기록
 *
 *   1) 세션 TTL 연장(keep-alive)
 *      - 웹소켓을 쓰는 앱은 브라우저 탭이 켜진 채로 오래 유지되는 경우가 많다.
 *      - 일정 주기(예: 30초~수분)에 한 번씩 서버에 "나 아직 있어요(KeepAlive)"라고 신호를 보낼 때,
 *        사용자의 HTTP 세션 TTL도 같이 연장해 주면, 장시간 끊김 없이 안정적으로 사용할 수 있다.
 *
 *   2) 현재 활성 채널을 Redis에 저장(+TTL)
 *      - 사용자가 보고 있는 방을 Redis에 임시로 기록해두면 여러 가지 UX/운영 이점이 있다.
 *        (1) 푸시/알림 라우팅: 새 메시지가 올 때 "지금 그 방을 보고 있으면 읽음 처리/뱃지 억제" 같은 최적화 가능
 *        (2) 재접속 복구: 브라우저 새로고침/네트워크 끊김 후 다시 접속 시, 마지막에 열어 뒀던 방으로 자동 복원
 *        (3) 청소 자동화: TTL을 짧게 두면 사용자가 떠난 후(웹을 닫은 후) 키가 자동으로 사라져서 고아 데이터가 남지 않음
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    /**
     * Spring Session이 제공하는 세션 저장소 추상화.
     * - 현재 설정에 따라 메모리/Redis/JDBC 등 다양한 백엔드를 사용할 수 있다.
     * - httpSessionId로 세션을 찾아서 "마지막 접근 시간(lastAccessedTime)"을 갱신하는 데 사용한다.
     */
    private final SessionRepository<? extends Session> httpSessionRepository;

    /**
     * Redis 접근용 템플릿.
     * - 현재 Raw 타입으로 선언되어 있지만, 권장 타입은 RedisTemplate<String, String>이다.
     *   (제네릭을 명시하면 캐스팅 문제와 경고를 줄일 수 있습니다)
     */
    private final RedisTemplate stringRedisTemplate;

    /**
     * Redis 키 네임스페이스.
     * - 최종 키는 "message:user:{userId}:{channel_id}" 형태가 된다.
     *   예) message:user:12345:channel_id  → 값: "67890" (채널ID)
     */
    private final String NAMESPACE = "message:user";

    /**
     * TTL(Time To Live) 설정(초 단위).
     * - 여기서는 300초(=5분)로 설정.
     * - 이 시간 동안만 "활성 채널" 키가 유지되고, 이후 자동으로 삭제.
     * - KeepAlive로 주기적으로 연장해 주면 계속 살아있다.
     */
    private final long TTL = 300;


    /**
     * 현재 세션에서 로그인한 사용자의 username을 가져온다.
     * : 로그인 사용자(username)를 SecurityContext에서 가져온다.
     *
     * @return 로그인한 사용자의 username
     */
    public String getUsername(){
        //현재 연결되어 있는 세션, 내 세션에서 내 이름(username)이 필요함 -> security의 도움을 받을 수 있음
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return username;
    }

    /**
     * 이 메서드는 전닯받은 httpSessionId를 이용해 세션을 찾고, 해당 세션이 존재한다면 마지막 접근 시간을 현재 시각으로 갱신한다(TTL 초기화 효과)
     *
     * 이는 TTL(Time To Live)을 연장하는 효과를 가져오며,
     * 세션이 만료되지 않도록 유지(keep-alive)하는데 사용된다
     * @param httpSessionId 클라이언트의 HttpSession ID(String 타입)
     */
    /**
     * 전달받은 httpSessionId를 이용해 세션을 찾고, 해당 세션이 존재한다면 마지막 접근 시간을 현재 시각으로 갱신(TTL 초기화 효과)
     * : HTTP 세션 TTL(유효 시간)을 연장하고, Redis의 "활성 채널" 키 TTL도 함께 연장한다.
     *      - 보통 WebSocket KeepAlive 요청 시점에 호출된다.
     *
     * ■ 카카오톡 비유
     *   - 앱이 주기적으로 "나 살아있어요" 핑을 보내면, 서버가 로그인 세션 만료 시간을 조금 뒤로 밀어준다.
     *   - 동시에 "지금 A방을 보고 있음" 정보도 잠깐 더 살려둔다. (5분 후 자동 삭제되지만, KeepAlive마다 갱신)
     *
     * @param userId        현재 사용자 식별자(우리 도메인의 UserId 값)
     * @param httpSessionId 브라우저의 HTTP 세션 ID
     */
    public void refreshTTL(UserId userId, String httpSessionId){
        // Redis에서 사용할 활성 채널 키를 구성. (예: message:user:12345:channel_id)
        String channelIdKey = buildChannelIdKey(userId);
        log.info("##### SessionService > refreshTTL method; channelKey: {} #####", channelIdKey);

        try{
            // 1) HTTP 세션을 세션 저장소에서 찾고
            Session httpSession = httpSessionRepository.findById(httpSessionId);

            // 2) 세션이 존재하면 "마지막 접근 시각"을 현재 시각으로 갱신 → Spring Session이 TTL을 연장함
            if(httpSession != null){
                httpSession.setLastAccessedTime(Instant.now()); // 세션 타임아웃 시간 계산의 기준점 업데이트

                // 3) Redis의 활성 채널 키도 TTL을 함께 연장 (둘을 동일한 주기로 묶어 유지)
                stringRedisTemplate.expire(channelIdKey, TTL, TimeUnit.SECONDS);
            }
        }catch(Exception ex){
            // expire 실패 시에도 서비스 전체가 죽을 필요는 없으므로 로깅만 합니다.
            log.error("Redis expire failed. key: {}", channelIdKey);
        }

        //만약 세션이 존재하지 않으면 아무 작업도 하지 않음(예. 세션이 만료됐거나 잘못된 ID)
    }


    /**
     * Redis 키를 만드는 유틸 메서드.
     *
     * @param userId 현재 사용자 식별자
     * @return "message:user:{userId}:{channel_id}" 형태의 키 문자열
     *
     *  중요 (잠재 버그 지적)
     *   - 현재 코드: "%s:%d:%s".formatted(NAMESPACE, userId, IdKey.CHANNEL_ID)
     *     1) %d는 숫자(Long 등) 자리인데 userId는 'UserId 객체'라서 IllegalFormatConversionException 가능
     *     2) IdKey.CHANNEL_ID 자체를 %s로 넣으면 "CHANNEL_ID" 텍스트가 들어간다.
     *        의도는 "channel_id" (소문자) 이므로 getValue()를 써야한다.
     *
     *   - 권장 코드: "%s:%d:%s".formatted(NAMESPACE, userId.id(), IdKey.CHANNEL_ID.getValue())
     *     → 예: message:user:12345:channel_id
     */
    private String buildChannelIdKey(UserId userId){

        return "%s:%d:%s".formatted(NAMESPACE, userId, IdKey.CHANNEL_ID);
    }



    /**
     * 사용자의 "현재 활성 채널"을 Redis에 기록. (값 = channelId, TTL = 5분)
     *
     * 언제 호출되나요?
     *   - 사용자가 특정 채널(대화방)에 "입장"할 때 ChannelService.enter()에서 호출힌다.
     *   - 이후 KeepAlive가 올 때마다 refreshTTL()에서 expire로 TTL을 연장한둔.
     *
     * 카카오톡 비유
     *   - 유저가 A와의 대화방을 열면, 서버에 "OOO님은 지금 A방을 보고 있어요"라고 적어다.
     *   - 새 메시지가 왔을 때 "이미 그 방을 보고 있으면 별도 알림 뱃지를 안 붙인다" 같은 로직을 만들 수 있다.
     *
     * @param userId    사용자 식별자
     * @param channelId 현재 들어간 채널 식별자
     * @return Redis set 성공 여부
     */
    public boolean setActiveChannel(UserId userId, ChannelId channelId){
        //키 만들기
        String channelIdKey = buildChannelIdKey(userId);

        try{
            stringRedisTemplate.opsForValue().set(channelIdKey, channelId.id().toString(), TTL, TimeUnit.SECONDS);
            return true;
        }catch(Exception ex){
            log.error("Redis set failed. key: {}, channelId: {}", channelIdKey, channelId);
            return false;
        }
    }

}
