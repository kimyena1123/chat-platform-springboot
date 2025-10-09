package com.chatting.backend.service;

import com.chatting.backend.constant.RedisKeyPrefix;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.InviteCode;
import com.chatting.backend.dto.domain.User;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.projection.UserIdUsernameInviterUserIdProjection;
import com.chatting.backend.entity.UserConnectionEntity;
import com.chatting.backend.json.JsonUtil;
import com.chatting.backend.repository.UserConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * [UserConnectionService]
 * - 사용자 간 채팅 연결 요청, 수락, 거절, 연결끊기 및 상태 조회를 처리하는 서비스
 * - DB user_connection 테이블을 기반으로 동작
 * - partnerA / partnerB 순서를 항상 작은 ID를 A, 큰 ID를 B로 canonical ordering 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserConnectionService {

    private final UserService userService;
    private final UserConnectionLimitService userConnectionLimitService;
    private final CacheService cacheService;
    private final UserConnectionRepository userConnectionRepository;
    private final JsonUtil jsonUtil;
    private final long TTL = 600; // 10분 : 상태가 변하기 때문


    /**
     * [연결 목록 조회 메서드]
     * <p>
     * 목적:
     * - 로그인한 사용자(파라미터 userId)에 대해, 특정 연결 상태(status)를 가진 '상대 사용자 목록'을 반환한다.
     * - 예: status = ACCEPTED 이면 "나와 연결된(친구가 된) 모든 사용자" 목록을 반환.
     * <p>
     * 왜 두 쿼리인가?:
     * - user_connection 테이블은 (partner_a_user_id, partner_b_user_id)를 복합키로 사용한다.
     * - 서비스 레이어에서 canonical ordering(항상 작은 id를 partnerA, 큰 id를 partnerB)으로 저장/조회하기 때문에,
     * "나"가 partnerA인 행과 "나"가 partnerB인 행이 따로 존재할 수 있다.
     * - JPQL은 UNION 같은 문법을 편하게 지원하지 않으므로(혹은 네이티브 SQL을 쓰지 않는 한),
     * 일반적으로 "내가 A인 경우"와 "내가 B인 경우" 두 쿼리를 각각 실행하여 결과를 합친다.
     *
     * @param userId 조회 대상(로그인한 사용자=나)
     * @param status 조회하고자 하는 연결 상태 (예: PENDING, ACCEPTED, REJECTED, NONE, DISCONNECTED)
     * @return 해당 상태에 있는 상대 사용자들의 List<User> (각 User는 userId + username)
     */
    @Transactional(readOnly = true) // 두 개의 쿼리를 동시에 사용
    public List<User> getUserByStatus(UserId userId, UserConnectionStatus status) {

        // Redis(캐시)에서 key 값을 가져온다
        String key = cacheService.buildKey(RedisKeyPrefix.CONNECTIONS_STATUS, userId.id().toString(), status.name()); // pending인지 accepted인ㅇ지
        //Redis에서 해당 key에 대한 value를 조회해 가져온다
        Optional<String> cachedUsers = cacheService.get(key);

        // value가 존재하면 그대로 리턴하고,
        // 없으면 Db에서 조회한 후 Redis에 set한다.
        if(cachedUsers.isPresent()) {
            // List<User> 구조이므로 JSON 문자열을 다시 객체 리스트로 역직렬화
            // user 클래스의 list니까 user를 넘겨준다.
            return jsonUtil.fromJsonToList(cachedUsers.get(), User.class);
        }


        // 1) 내가 partnerA(작은 id)인 관계들: 여기서 반환되는 projection은 "상대 = partnerB" 의 id와 username을 담고 있다.
        //    SQL/JPQL 레벨에서 user_connection.u.partnerB_user_id 와 user.username 을 조인해서 가져옴.
        List<UserIdUsernameInviterUserIdProjection> usersA = userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), status);
        // 2) 내가 partnerB(큰 id)인 관계들: 여기서 반환되는 projection은 "상대 = partnerA" 의 id와 username을 담고 있다.
        //    즉, repository 메서드는 반대편 칼럼을 기준으로 조인하도록 작성되어 있다.
        List<UserIdUsernameInviterUserIdProjection> usersB = userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), status);

        List<User> fromDB;

        if (status == UserConnectionStatus.ACCEPTED) {
            // ACCEPTED 상태일 때는 초대한 사람/초대받은 사람 모두 포함
            fromDB = Stream.concat(usersA.stream(), usersB.stream())
                    .map(item -> new User(new UserId(item.getUserId()), item.getUsername()))
                    .toList();

        } else {
            // PENDING, REJECTED 등은 초대한 사람 본인은 목록에 포함되지 않도록 필터링
            fromDB = Stream.concat(usersA.stream(), usersB.stream())
                    .filter(item -> !item.getInviterUserId().equals(userId.id()))
                    .map(item -> new User(new UserId(item.getUserId()), item.getUsername()))
                    .toList();
        }

        // DB 결과를 Redis에 JSON 형태로 저장 (조회 성능 향상)
        if (!fromDB.isEmpty()) {
            jsonUtil.toJson(fromDB).ifPresent(json -> cacheService.set(key, json, TTL));
        }

        return fromDB;
    }


    //내가 10명의 사용자에게 그룹 초대를 보낸다고 했을 때, 이미 ACCEPTED 상태인 사람이 몇명인지, PENDING 샅애인 사람이 몇명인지 한번에 세고 싶을 때 사용
    //특정 사용자(A) 와 여러 명의 사용자 집합(B 리스트) 사이의 관계를 한 번에 카운트할 때
    //B들 중 몇 명이 A와 특정 상태(status)에 있는가?”
    @Transactional(readOnly = true) // 두 개의 쿼리를 동시에 사용
    public long countConnectionStatus(UserId senderUserId, List<UserId> partnerUserIds, UserConnectionStatus status){
        List<Long> ids = partnerUserIds.stream().map(UserId::id).toList();

        return userConnectionRepository.countByPartnerAUserIdAndPartnerBUserIdInAndStatus(senderUserId.id(), ids, status) +
                userConnectionRepository.countByPartnerBUserIdAndPartnerAUserIdInAndStatus(senderUserId.id(), ids, status);
    }


    /**
     * [연결 초대(요청) 메서드 : 친구 맺기 : 친구 초대]
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
    //invite는 쓰기니까 해당 안된다.
    @Transactional //setStatus() 사용
    public Pair<Optional<UserId>, String> invite(UserId inviterUserId, InviteCode inviteCode) {
        //1. 초대코드(inviteCode)로 파트너(초대 대상) 찾기
        //User = usersId + username
        Optional<User> partner = userService.getUser(inviteCode);

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
                if (userService
                        .getConnectionCount(inviterUserId)
                        .filter(count -> count >= userConnectionLimitService.getLimitConnections())
                        .isPresent()) {
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
                    // 비즈니스 규칙 위반(예: connection limit 초과) 등으로 인해 수락 불가능한 경우
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

                    log.error("Set pending failed. cause: {}", ex.getMessage());
                    yield Pair.of(Optional.empty(), "InviteRequest failed.");
                }
            }
            // 이미 연결(수락)되어 있으면 다시 초대할 필요 없음 -> 사용자에게 알림용 메시지 반환
            case ACCEPTED ->
                    Pair.of(Optional.empty(), "Already connected with " + partnerUsername); //이미 연결이 된 상태이기에 에러 메시지 출력 //이미 연결된 상태에서 요청이 들어왔을 때의 상황

            // 이미 초대 중이거나 거절된 상태면 중복 알림 방지
            case PENDING, REJECTED -> {
                log.info(
                        "{} invites {} but does not deliver the invitation request.",
                        inviterUserId,
                        partnerUsername);
                yield Pair.of(Optional.empty(), "Already invited to " + partnerUsername); //이미 초대가 된 상황인데 또 보낸(초대 요청을) 상황
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
    @Transactional
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
        } catch (IllegalStateException ex) {
            if(TransactionSynchronizationManager.isActualTransactionActive()){ //현재 트랜잭션이 활성화되어 있는 상태인지
                // 비즈니스 규칙 위반(예: connection limit 초과) 등으로 인해 수락 불가능한 경우
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return Pair.of(Optional.empty(), ex.getMessage());
        } catch (Exception ex) {
            if(TransactionSynchronizationManager.isActualTransactionActive()){ //현재 트랜잭션이 활성화되어 있는 상태인지
                // 비즈니스 규칙 위반(예: connection limit 초과) 등으로 인해 수락 불가능한 경우
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }

            // DB에서 필요한 데이터(유저 혹은 user_connection)를 찾지 못한 경우
            log.error("Accept failed. cause: {}", ex.getMessage());
            return Pair.of(Optional.empty(), "Accept failed.");
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
    @Transactional //setStatus() 사용
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
                        // 비즈니스 규칙 위반(예: connection limit 초과) 등으로 인해 수락 불가능한 경우
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

                        log.error("Set rejected failed. cause: {}", ex.getMessage());
                        return Pair.of(false, "Reject failed.");
                    }
                }).orElse(Pair.of(false, "Reject failed."));
    }

    /**
     * [연결 끊기 메서드]
     * <p>
     * 반환값:
     * 성공: true, 연결끊기는 사람의 username(상대방 username)
     * 실패: false, 에러 메시지
     *
     * @param senderUserId    연결 끊는 사람
     * @param partnerUsername 연결 끊기는 사람(상대방)
     */
    @Transactional
    public Pair<Boolean, String> disconnect(UserId senderUserId, String partnerUsername) {
        // 1) partnerUsername → partnerUserId 로 변환 시도
        return userService
                .getUserId(partnerUsername)
                // 2) senderUserId와 partnerUserId가 같다면 "자기 자신을 끊기"가 되므로 허용하지 않는다.
                .filter(partnerUserId -> !senderUserId.equals(partnerUserId))
                // 3) map: 여기서부터는 "조건이 통과된 partnerUserId"에 대해 실제 끊기 로직을 수행.
                .map(
                        partnerUserId -> {
                            try {
                                // 3-1) 현재 상태 조회
                                UserConnectionStatus userConnectionStatus = getStatus(senderUserId, partnerUserId);

                                // 3-2) 케이스 A: 현재 ACCEPTED(서로 연결된 상태)라면
                                if (userConnectionStatus == UserConnectionStatus.ACCEPTED) {
                                    userConnectionLimitService.disconnect(senderUserId, partnerUserId);
                                    return Pair.of(true, partnerUsername);
                                }
                                // 3-3) 케이스 B: 현재 REJECTED이고, 과거 DB에 기록된 '초대한 사람(inviter)'이 partner였다면(즉, "상대가 나에게 초대한 걸 내가 거절한 상태")
                                // REJECTED 상태이면, 다시 연결 요청을 못한다. 그렇기에 DISCONNECTED 상태로 바꿔서 다시 요청할 수 있도록 한다.
                                else if (userConnectionStatus == UserConnectionStatus.REJECTED &&
                                        getInviterUserId(senderUserId, partnerUserId)
                                                .filter(inviterUserId -> inviterUserId.equals(partnerUserId)).isPresent()) {
                                    setStatus(senderUserId, partnerUserId, UserConnectionStatus.DISCONNECTED);
                                    return Pair.of(true, partnerUsername);
                                }
                            } catch (Exception ex) {
                                if(TransactionSynchronizationManager.isActualTransactionActive()){ //현재 트랜잭션이 활성화되어 있는 상태인지
                                    // 비즈니스 규칙 위반(예: connection limit 초과) 등으로 인해 수락 불가능한 경우
                                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                                }
                                // 예외(트랜잭션, DB 이슈 등) 발생 시 로깅하고 실패 반환
                                log.error("Disconnect failed. cause: {}", ex.getMessage());
                            }

                            // 위 조건 어디에도 해당하지 않으면 실패
                            return Pair.of(false, "Disconnect failed.");
                        })
                // 4) Optional이 비어있다면(= partnerUsername → userId 변환 실패 or 자기 자신 체크에서 탈락)
                // -> 실패 반환
                .orElse(Pair.of(false, "Disconnect failed."));
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
     *
     * private -> public으로 변경: 왜? 이제 UserConnectionService에서만 쓰는게 아닌 Channel 쪽에서도 사용하기 때문.
     */
    @Transactional(readOnly = true) //DB 조작은 없고, DB 조회한다
    private UserConnectionStatus getStatus(UserId inviterUserId, UserId partnerUserId) {

        long partnerA = Long.min(inviterUserId.id(), partnerUserId.id());
        long partnerB = Long.max(inviterUserId.id(), partnerUserId.id());

        String key = cacheService.buildKey(RedisKeyPrefix.CONNECTION_STATUS, String.valueOf(partnerA), String.valueOf(partnerB));
        Optional<String> cachedConnectionStatus = cacheService.get(key);

        if (cachedConnectionStatus.isPresent()) {
            return UserConnectionStatus.valueOf(cachedConnectionStatus.get());
        }

        // repository에서 (partnerA, partnerB)로 찾고, 존재하면 상태 문자열을 enum으로 변환해서 반환
        UserConnectionStatus fromDB = userConnectionRepository.findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(partnerA, partnerB)
                .map(status -> UserConnectionStatus.valueOf(status.getStatus()))
                .orElse(UserConnectionStatus.NONE); // 없으면 NONE

        // 항상 Redis에 set (값이 NONE이라도 캐시해 두면 다음 호출 시 DB 부하 감소)
        cacheService.set(key, fromDB.name(), TTL);

        return fromDB;
    }


    /**
     * [현재 상태 갱신 메서드]
     * : 두 사용자 간의 상태를 갱신하여 DB에 저장한다
     * <p>
     * - ACCEPTED는 여기서 직접 바꾸지 않고 다른 서비스(userConnectionLimitService.accept)에서 처리
     * → 이유: 연결 개수 제한 등 비즈니스 로직을 거쳐야 하므로 방어 코드 필요
     */
    //setStatus쪽은 만료시켜줘야 한다. 찾아서 변경된 상태값을 캐시에서 삭제시켜 줘야 한다.
    //여기는 redis에 세팅X.
    //redis에 있는 값을 삭제
    @Transactional
    private void setStatus(UserId inviterUserId, UserId partnerUserId, UserConnectionStatus userConnectionStatus) {

        //ACCETED로 바꿀 수 없게 방어코드 만들기
        //1000명까지의 remit(제한)이 있다. 여기서 업데이트를 이 Transactional에서는 못막는다. 이 로직을 가지고는 막기 힘들어서 여기서는 ACCEPTED를 튕겨내고 다른 쪽에서 ACCEPTED 처리 할 예정.
        if (userConnectionStatus == UserConnectionStatus.ACCEPTED) {
            throw new IllegalArgumentException("Can't set to accepted.");
        }

        //setStatus()가 호출되면 바꿀 상태값을 DB에 저장하고,
        //DB에 저장된 값이랑 관련된 값들을 cache에서 삭제시켜준다.

        long partnerA = Long.min(inviterUserId.id(), partnerUserId.id());
        long partnerB = Long.max(inviterUserId.id(), partnerUserId.id());

        //save(A, B, 바꿀 상태값, 초대한 사람의 userId 저장)
        userConnectionRepository.save(new UserConnectionEntity(partnerA, partnerB, userConnectionStatus, inviterUserId.id()));

        // 상태가 바뀌면 관련 캐시 삭제 (CONNECTION_STATUS, CONNECTIONS_STATUS)
        cacheService.delete(List.of(
                // 두 사람 관계의 상태 캐시 삭제(너와 나의 연결 관계 삭재)
                cacheService.buildKey(RedisKeyPrefix.CONNECTION_STATUS, String.valueOf(partnerA), String.valueOf(partnerB)),
                // 내 친구 목록 삭제(상태가 변경됐으니 상태 목록도 바뀌기에 삭제해야 한다)
                cacheService.buildKey(RedisKeyPrefix.CONNECTIONS_STATUS, inviterUserId.id().toString(), userConnectionStatus.name()),
                // 상대방의 목록 삭제(상태가 변경됐으니 상태 목록도 바뀌기에 삭제해야 한다)
                cacheService.buildKey(RedisKeyPrefix.CONNECTIONS_STATUS, partnerUserId.id().toString(), userConnectionStatus.name())
        ));
    }

    /**
     * [DB에 저장된 초대자 userId 조회 메서드]
     * : 두 사용자 관계에서 실제 초대를 보낸 사람이 누구인지 확인하는 메서드
     * <p>
     * → 수락/거절 시 클라이언트가 보낸 inviterUsername이 실제 초대자와 일치하는지 검증하는데 사용
     */
    @Transactional(readOnly = true) //DB 조작은 없고, DB 조회한다
    private Optional<UserId> getInviterUserId(UserId partnerAUserId, UserId partnerBUserId) {
        long partnerA = Long.min(partnerAUserId.id(), partnerBUserId.id());
        long partnerB = Long.max(partnerBUserId.id(), partnerAUserId.id());

        //key 가져오기
        //2개 사용자에 대한 초대한 사람이 필요한 거니까 key를 user_id 두 개를 붙여서 DB의 복합키처럼 사용하는 것과 같은 방식이다.
        String key = cacheService.buildKey(RedisKeyPrefix.INVITER_USER_ID, String.valueOf(partnerA), String.valueOf(partnerB));
        Optional<String> cachedInviterUserId = cacheService.get(key);

        if(cachedInviterUserId.isPresent()){
            //String이라서 UserId를 만들어야 한다.
            return Optional.of(new UserId(Long.valueOf(cachedInviterUserId.get())));
        }

        Optional<UserId> fromDB = userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(partnerA, partnerB)
                .map(inviterUserId -> new UserId(inviterUserId.getInviterUserId()));

        fromDB.ifPresent(userId -> cacheService.set(key, userId.id().toString(), TTL));

        return fromDB;
    }


}