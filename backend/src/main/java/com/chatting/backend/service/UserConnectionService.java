package com.chatting.backend.service;

import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.InviteCode;
import com.chatting.backend.dto.domain.User;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.projection.UserIdUsernameProjection;
import com.chatting.backend.entity.UserConnectionEntity;
import com.chatting.backend.repository.UserConnectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * [동작흐름]
 * 1. 초대 요청(invite)을 받는다: inviteUserId(초대자 ID)가 상대방 inviteCode(초대코드)로 채팅 요청
 * 2. inviteCode로 초대 대상(초대코드를 가진 사용자)를 찾아 partner(초대코드를 가진 사용자)의 UserId, username을 얻는다
 * 3. 자기 자신에게 초대한 것인지 아닌지 검사한다
 * 4. 현재 두 사용자(초대한 자, 초대받은 자) 간의 상태를 조회(getStatus)를 조회한다.
 * 5. if 상태가 NONE or DISCONNECTED이면: PENDING으로 바꾸고 초대받은 자의 userid와 초대한 자의 username을 Pair로 반환
 * 6  if 상태가 ACCEPTED이면: 이미 연결됐다는 문구 출력
 * 7. if 상태가 PENDING or REJECTED이면: 이미 초대했다는 문구 출력
 * <p>
 * [예시]
 * user1 (userId=1, inviteCode="123456abc")
 * user2 (userId=2, inviteCode="987654cba")
 * <p>
 * 만약 user2가 user1을 초대하고자 한다면:
 * invite(inviterUserId=2, inviteCode="123456abc") 호출
 * userService.getUser("123456abc") -> partnerUserId = 1
 * getStatus(2, 1) 호출 -> 현재 NONE 이라면 setStatus(2, 1, PENDING) 호출
 * - DB에 (partnerA=1, partnerB=2, status=PENDING, inviterUserId=2) 저장
 * 반환값: Pair.of(Optional.of( partnerUserId(=1) ), inviterUsername("user2's name"))
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserConnectionService {

    private final UserService userService;
    private final UserConnectionRepository userConnectionRepository;
    private final UserConnectionLimitService userConnectionLimitService;


    /**
     * [연결 목록 조회 메서드]
     *
     * 목적:
     *   - 로그인한 사용자(파라미터 userId)에 대해, 특정 연결 상태(status)를 가진 '상대 사용자 목록'을 반환한다.
     *   - 예: status = ACCEPTED 이면 "나와 연결된(친구가 된) 모든 사용자" 목록을 반환.
     *
     * 왜 두 쿼리인가?:
     *   - user_connection 테이블은 (partner_a_user_id, partner_b_user_id)를 복합키로 사용한다.
     *   - 서비스 레이어에서 canonical ordering(항상 작은 id를 partnerA, 큰 id를 partnerB)으로 저장/조회하기 때문에,
     *     "나"가 partnerA인 행과 "나"가 partnerB인 행이 따로 존재할 수 있다.
     *   - JPQL은 UNION 같은 문법을 편하게 지원하지 않으므로(혹은 네이티브 SQL을 쓰지 않는 한),
     *     일반적으로 "내가 A인 경우"와 "내가 B인 경우" 두 쿼리를 각각 실행하여 결과를 합친다.
     *
     * 반환 타입:
     *   - User: 도메인 DTO (UserId + username).
     *
     * 구현 요약(단계):
     *   1) repository.findByPartnerAUserIdAndStatus(userId, status) 호출 -> 내가 partnerA인 쪽(상대는 partnerB)
     *   2) repository.findByPartnerBUserIdAndStatus(userId, status) 호출 -> 내가 partnerB인 쪽(상대는 partnerA)
     *   3) 두 결과를 Stream.concat으로 합치고, 각 projection 요소를 도메인 User로 변환
     *   4) 리스트로 수집하여 반환
     *
     * 주의/확인 사항:
     *   - Projection( UserIdUsernameProjection )은 DB에서 필요한 컬럼(userId, username)만 가져오기 때문에 성능상 이득(특히 많은 컬럼이 있는 엔티티일 때)
     *   - 합친 후 중복 제거가 필요한지(동일 상대가 두 번 들어올 가능성) 확인: canonical ordering이 올바르게 유지되면 중복이 없어야 한다.
     *   - 정렬(ordering)이 필요하면 서비스 레이어에서 정렬을 추가할 수 있다.
     *
     * @param userId 조회 대상(로그인한 사용자=나)
     * @param status 조회하고자 하는 연결 상태 (예: PENDING, ACCEPTED, REJECTED, NONE, DISCONNECTED)
     * @return 해당 상태에 있는 상대 사용자들의 List<User> (각 User는 userId + username)
     */
    public List<User> getUserByStatus(UserId userId, UserConnectionStatus status) {
        // 1) 내가 partnerA(작은 id)인 관계들: 여기서 반환되는 projection은 "상대 = partnerB" 의 id와 username을 담고 있다.
        //    SQL/JPQL 레벨에서 user_connection.u.partnerB_user_id 와 user.username 을 조인해서 가져옴.
        List<UserIdUsernameProjection> usersA = userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), status);
        // 2) 내가 partnerB(큰 id)인 관계들: 여기서 반환되는 projection은 "상대 = partnerA" 의 id와 username을 담고 있다.
        //    즉, repository 메서드는 반대편 칼럼을 기준으로 조인하도록 작성되어 있다.
        List<UserIdUsernameProjection> usersB = userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), status);

        // 3) 두 리스트를 합치고(UserIdUsernameProjection -> User 도메인으로 매핑)
        //    Stream.concat 를 사용하면 성능상 이점(중간 컬렉션 재할당을 최소화)과 가독성을 얻을 수 있다.
        //    각 projection의 getUserId() / getUsername() 값을 새로운 User 도메인 객체로 변환하여 반환한다.
        return Stream
                .concat(usersA.stream(), usersB.stream())
                // map 단계: Projection -> 도메인 User
                //   - UserIdUsernameProjection.getUserId() 는 DB에서 가져온 상대방의 numeric id
                //   - new User(new UserId(...), username) 는 내부 도메인 DTO를 구성하는 표준 방식
                .map(item -> new User(new UserId(item.getUserId()), item.getUsername()))
                .toList();
    }


    /**
     * [채팅 초대(요청) 메서드]
     * : inviterUserId(초대한 사람)가 inviteCode(상대방 초대코드)를 가지고 초대 요청을 보낼 때 호출된다
     * <p>
     * 초대하는 사람은 상대방의 userId를 모르기에 초대코드(inviteCode)를 가지고 진행한다
     * 초대 가능한 상태이면, DB에 PENDING 저장하고 초대받은 사람의 userId와 초대한 자의 username을 반환한다.
     * <p>
     * * 반환값:
     * * 초대 성공: [초대받은 사람의 userId, 초대한 사람(inviter)의 username]
     * * 초대 실패: [empty 값 , errorMessage] 또는 [초대받은 사람의 userId, errorMessage]
     *
     * @param inviterUserId 초대요청을 보내는 사람의 userId
     * @param inviteCode    초대요청을 받는 사람의 inviteCode
     */
    public Pair<Optional<UserId>, String> invite(UserId inviterUserId, InviteCode inviteCode) {
        //1. 초대코드(inviteCode)로 파트너(초대 대상) 찾기
        //User = usersId + username
        Optional<User> partner = userService.getUser(inviteCode); //getUser: 초대코드로 username을 찾는 메서드.

        //2. 파트너 없음: 잘못된 초태코드: 잘못된 요청이 들어왔을 때(사용자가 보낸 초대코드와 실제 상대방의 초대코드가 다른 상황)
        if (partner.isEmpty()) {
            log.info("Invalid invite code. {}, from {}", inviteCode, inviterUserId);

            return Pair.of(Optional.empty(), "Invalid invite code.");
        }

        //Optional에서 값 추출
        UserId partnerUserId = partner.get().userId();
        String partnerUsername = partner.get().username();

        //3. 자기 자신에게 보낸 초대인지 검사(자기 자신을 초대할 수 없음)
        if (partnerUserId.equals(inviterUserId)) {
            return Pair.of(Optional.empty(), "Can't self invite.");
        }

        //4. 현재 두 사람 간 상태 조회(NONE, PENDING, ACCEPTED, DISCONNECTED, REJECTED)
        UserConnectionStatus userConnectionStatus = getStatus(inviterUserId, partnerUserId);

        //5. 상태에 따른 분기 처리
        return switch (userConnectionStatus) {
            case NONE, DISCONNECTED -> {
                //연결 한도에 도달했는지 확인
                //초대자의 연결 한도 도달했는지 확인
                if (userService.getConnectionCount(inviterUserId).filter(count -> count >= userConnectionLimitService.getLimitConnections()).isPresent()) {
                    yield Pair.of(Optional.empty(), "Connection limit reached.");
                }

                //초대자의 이름 가져오기
                //getUsername: userId로 username을 찾는 메서드
                Optional<String> inviterUsername = userService.getUsername(inviterUserId);

                //초대한 사람의 username을 못찾으면 실패 처리(초대자를 못찾은 상태)
                if (inviterUsername.isEmpty()) {
                    log.warn("InviteRequest failed.");
                    yield Pair.of(Optional.empty(), "InviteRequest failed.");
                }

                //이름이 비어있지 않다면,
                //사용자가 유효하다는 의미이니까, setStatus로 상태를 변경.
                try {
                    //NONE, DISCONNECTED: 연결이 안된 상태이기에 연결을 요청한 상황 -> PENDING 상태로 저장(초대 요청 등록)
                    //상대방이 수락해야 ACCEPTED가 되는 것임
                    setStatus(inviterUserId, partnerUserId, UserConnectionStatus.PENDING);

                    // 초대한 사람 입장: 초대받을 사람의 userId를 알아야 그 초대받은 사람의 세션을 찾아 알림(webSocket) 전송 가능
                    // 초대받은 사람 입장: 누가 자신을 초대했는지 초대자의 이름을 알아야 함 >> 그래서 inviterUsername이 필요
                    yield Pair.of(Optional.of(partnerUserId), inviterUsername.get());
                } catch (Exception ex) {
                    log.error("Set PENDING Failed. cause: {}", ex.getMessage());
                    yield Pair.of(Optional.empty(), "Set PENDING Failed.");
                }
            }
            // 이미 연결(수락)되어 있으면 다시 초대할 필요 없음 -> 사용자에게 알림용 메시지 반환
            case ACCEPTED ->
                    Pair.of(Optional.of(partnerUserId), "Already connected with " + partnerUsername);//이미 연결이 된 상태이기에 에러 메시지 출력 //이미 연결된 상태에서 요청이 들어왔을 때의 상황
            // 이미 초대 중이거나 거절된 상태면 중복 알림 방지
            case PENDING, REJECTED -> {
                log.info("{} invites {} but does not deliver the invitation request.", inviterUserId, partnerUsername);
                yield Pair.of(Optional.of(partnerUserId), "Already invited to " + partnerUsername);  //이미 초대가 된 상황인데 또 보낸(초대 요청을) 상황
            }
        };

    }


    /**
     * [채팅 초대 수락 메서드]
     * : 초대받은 사람이 초대를 수락할 때 호출된다.
     * <p>
     * 흐름:
     * 1. 초대한 사람(inviter)이 실제로 존재하는지 확인
     * 2. 수락자와 초대한 사람이 동일한 사림이 아닌지 확인(자기자신 수락방지) 확인
     * 3. DB에 기록된 '초대한 사람(inviter_user_id)'와 클라이언트가 보낸 inviterUsername(역추적된 userId)이 일치하는지 비교
     * 4. 현재 상태가 PENDING인지 확인(그 외 상태일 경우 겨부)
     * 5. 수락자와 초대한 사람의 connectionCount 한도(limit)을 확인
     * 6. 한도 통과시 실제로 ACCEPTED로 상태 변경(=userConnectionLimitService.accept 내부에서 처리)
     * <p>
     * 반환값:
     * 수락 성공: [초대한 사람의 userId, 초대수락하는 사람의 username]
     * 수락 실패: [empty 값, errorMessage]
     *
     * @param acceptorUserId  수락하는 사람(초대를 받은 사람)의 userId
     * @param inviterUsername 초대한 사람의 username
     */
    public Pair<Optional<UserId>, String> accept(UserId acceptorUserId, String inviterUsername) {
        //1. inviterUsername -> inviterUserId로 변환(username으로 userId를 찾아옴)
        Optional<UserId> userId = userService.getUserId(inviterUsername);

        //2. 초대한 사람(inviter)이 존재하는지 확인
        if (userId.isEmpty()) {
            return Pair.of(Optional.empty(), "Invalid username."); // 잘못된 username(예: 변경되었거나 존재하지 않음)
        }

        //실제 inviterUserId
        UserId inviterUserId = userId.get();

        //3. 수락자와 초대한 사람이 동일한지 검사(자기 자신을 수락 불가)
        if (acceptorUserId.equals(inviterUserId)) {
            return Pair.of(Optional.empty(), "Can't self accept.");
        }

        //4. DB에 저장된 invite4r_user_id와 지금 요청에서 온 inviterUserId가 일치하는지 확인
        if (getInviterUserId(acceptorUserId, inviterUserId)
                .filter(invitationSenderUserId -> invitationSenderUserId.equals(inviterUserId))
                .isEmpty()) {
            return Pair.of(Optional.empty(), "Invalid username."); // DB에 저장된 초대한 사람 정보가 지금 요청에서 온 inviterUserId와 일치하지 않음 -> 거부
        }

        //5. 현재 두 사람의 관계 상태를 확인(PENDING이어야만 수락 가능)
        UserConnectionStatus userConnectionStatus = getStatus(inviterUserId, acceptorUserId);

        if (userConnectionStatus == UserConnectionStatus.ACCEPTED) {
            return Pair.of(Optional.empty(), "Already connected."); // 이미 수락되어 연결이 된 상태
        }

        if (userConnectionStatus != UserConnectionStatus.PENDING) {
            return Pair.of(Optional.empty(), "Accept failed."); //PENDING이 아니면 수락할 수 없다.
        }

        //6. 성공시 응답에 포함할 acceptor와 username을 미리 가져둔다.
        Optional<String> acceptorUsername = userService.getUsername(acceptorUserId);

        // acceptor UserId가 이상하거나 DB에 문제가 있는 경우
        if (acceptorUsername.isEmpty()) {
            log.error("Invalid userId. userId: {}", acceptorUserId);
            return Pair.of(Optional.empty(), "Accept failed.");
        }

        //7. 실제 "수락 처리" 수행
        try {
            userConnectionLimitService.accept(acceptorUserId, inviterUserId); //요청을 수락하면 이 메서드가 호출된다(수락자가 이 메서드를 호출한다고 보면 된다)

            //성공: 초대한 사람의 userId와 수락자 이름(=일림에 보낼 이름)을 반환
            return Pair.of(Optional.of(inviterUserId), acceptorUsername.get());
        } catch (EntityNotFoundException ex) {
            // DB에서 필요한 데이터(유저 혹은 user_connection)를 찾지 못한 경우
            log.error("Accept failed. cause: {}", ex.getMessage());
            return Pair.of(Optional.empty(), "Accept failed.");
        } catch (IllegalStateException ex) {
            // 비즈니스 규칙 위반(예: connection limit 초과) 등으로 인해 수락 불가능한 경우
            return Pair.of(Optional.empty(), ex.getMessage());
        }

    }


    /**
     * [채팅 요청 거절 메서드]
     * : 초대받은 사람이 초대를 거절할 때 호출된다.
     * <p>
     * 조건:
     * - 자기자신의 보낸 초대를 자기 자신이 거절하는 것은 불가능
     * - 상태가 PENDING일 때만 거절 가능
     * <p>
     * 반환값:
     * 거절 성공: [true, 초대한 사람의 username]
     * 거절 실패: [false, errorMessage]
     *
     * @param rejectorUserId  거절하는 사람(초대를 받은 사람)의 userId
     * @param inviterUsername 초대한 사람의 username
     */
    //accept() 메서드와 같이 실패조건들을 나열해서 상세하게 해도 되고,
    //reject() 메서드와 같이 간략하게 다 Reject failed라고 해도 된다. 편한 방식으로 개발하면 된다.
    public Pair<Boolean, String> reject(UserId rejectorUserId, String inviterUsername) {

        //rejectorUserId와 inviterUserId가 같지 않아야 않다.(같다면 스스로가 보낸 요청을 스스로 거절하는 꼴이 되는 것임)
        return userService.getUserId(inviterUsername)
                .filter(inviterUserId -> !inviterUserId.equals(rejectorUserId))
                .filter(inviterUserId -> getInviterUserId(inviterUserId, rejectorUserId).filter(invitationSenderUserId ->
                        invitationSenderUserId.equals(inviterUserId)).isPresent())
                //PENDING 상태에서만 reject이 가능하다
                .filter(inviterUserId -> getStatus(inviterUserId, rejectorUserId) == UserConnectionStatus.PENDING)
                .map(inviterUserId -> {
                    try {
                        setStatus(inviterUserId, rejectorUserId, UserConnectionStatus.REJECTED);

                        return Pair.of(true, inviterUsername);
                    } catch (Exception ex) {
                        log.error("Set rejected failed. cause: {}", ex.getMessage());
                        return Pair.of(false, "Reject failed.");
                    }
                }).orElse(Pair.of(false, "Reject failed."));
    }


    /**
     * [현재 상태 조회 메서드]
     * : 두 사용자 간의 현재 연결 상태(NONE, PENDING< ACCEPTED, REJECTED, DISCONNECTED)를 조회할 때 사용
     * <p>
     * 왜 Long.min/Long.max를 사용하는가?
     * - user_connection 테이블은 (partner_a_user_id, partner_b_user_id) 복합 PK 사용
     * - (A,B)와 (B,A)를 동일한 관계로 보기 위해 항상 작은 ID를 partnerA, 큰 ID를 partnerB로 정규화
     * <p>
     * 즉, (A,B)와 (B,A)를 같은 관계로 보기 위해 서비스 레이어에서 항상 작은 id를 partnerA로,
     * 큰 id를 partnerB로 정규화(canonical ordering)합니다. 따라서 조회 시에도 동일한 규칙 적용.
     *
     * @param inviterUserId 초대한 사람의 userId
     * @param partnerUserId 상대방의 userId
     * @return
     */
    private UserConnectionStatus getStatus(UserId inviterUserId, UserId partnerUserId) {
        // repository에서 (partnerA, partnerB)로 찾고, 존재하면 상태 문자열을 enum으로 변환해서 반환
        return userConnectionRepository.findUserConnectionStatusProjectionByPartnerAUserIdAndPartnerBUserId(
                        Long.min(inviterUserId.id(), partnerUserId.id()),
                        Long.max(inviterUserId.id(), partnerUserId.id()))
                .map(status -> UserConnectionStatus.valueOf(status.getStatus()))
                .orElse(UserConnectionStatus.NONE); // 없으면 NONE
    }


    /**
     * [현재 상태 갱신 메서드]
     * : 두 사용자 간의 상태를 갱신하여 DB에 저장한다
     * <p>
     * - ACCEPTED는 여기서 직접 바꾸지 않고 다른 서비스(userConnectionLimitService.accept)에서 처리
     * → 이유: 연결 개수 제한 등 비즈니스 로직을 거쳐야 하므로 방어 코드 필요
     */
    @Transactional
    private void setStatus(UserId inviterUserId, UserId partnerUserId, UserConnectionStatus userConnectionStatus) {

        //ACCETED로 바꿀 수 없게 방어코드 만들기
        //1000명까지의 remit(제한)이 있다. 여기서 업데이트를 이 Transactional에서는 못막는다. 이 로직을 가지고는 막기 힘들어서 여기서는 ACCEPTED를 튕겨내고 다른 쪽에서 ACCEPTED 처리 할 예정.
        if (userConnectionStatus == UserConnectionStatus.ACCEPTED) {
            throw new IllegalArgumentException("Can't set to accepted.");
        }

        userConnectionRepository.save(new UserConnectionEntity(
                Long.min(inviterUserId.id(), partnerUserId.id()),   // partnerA
                Long.max(inviterUserId.id(), partnerUserId.id()),   // partnerB
                userConnectionStatus, //비꿀 상태값
                inviterUserId.id() // 초대한 사람 ID 저장
        ));
    }

    /**
     * [DB에 저장된 초대자 userId 조회 메서드]
     * : 두 사용자 관계에서 실제 초대를 보낸 사람이 누구인지 확인하는 메서드
     * <p>
     * → 수락/거절 시 클라이언트가 보낸 inviterUsername이 실제 초대자와 일치하는지 검증하는데 사용
     */
    private Optional<UserId> getInviterUserId(UserId partnerAUserId, UserId partnerBUserId) {
        //SELECT inviter_user_id FROM user_connection WHERE partner_a_user_id = ? AND partner_b_user_id = ?
        return userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(
                //partnerAUserId와 partnerBUserId를 파라미터로 봤을 때 누가 inviter이고 누가 acceptor인지 모른다.
                Long.min(partnerAUserId.id(), partnerBUserId.id()),
                Long.max(partnerAUserId.id(), partnerBUserId.id())).map(inviterUserIdProjection -> new UserId(inviterUserIdProjection.getInviterUserId()));
    }


}
