package com.chatting.backend.service;

import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.InviteCode;
import com.chatting.backend.dto.domain.User;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.entity.UserConnectionEntity;
import com.chatting.backend.repository.UserConnectionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * [동작흐름]
 * 1. 초대 요청(invite)을 받는다: inviteUserId(초대자 ID)가 상대방 inviteCode(초대코드)로 채팅 요청
 * 2. inviteCode로 초대 대상(초대코드를 가진 사용자)를 찾아 partner(초대코드를 가진 사용자)의 UserId, username을 얻는다
 * 3. 자기 자신에게 초대한 것인지 아닌지 검사한다
 * 4. 현재 두 사용자(초대한 자, 초대받은 자) 간의 상태를 조회(getStatus)를 조회한다.
 * 5. if 상태가 NONE or DISCONNECTED이면: PENDING으로 바꾸고 초대받은 자의 userid와 초대한 자의 username을 Pair로 반환
 * 6  if 상태가 ACCEPTED이면: 이미 연결됐다는 문구 출력
 * 7. if 상태가 PENDING or REJECTED이면: 이미 초대했다는 문구 출력
 *
 * [예시]
 * user1 (userId=1, inviteCode="123456abc")
 * user2 (userId=2, inviteCode="987654cba")
 *
 * 만약 user2가 user1을 초대하고자 한다면:
 * invite(inviterUserId=2, inviteCode="123456abc") 호출
 * userService.getUser("123456abc") -> partnerUserId = 1
 * getStatus(2, 1) 호출 -> 현재 NONE 이라면 setStatus(2, 1, PENDING) 호출
 *  - DB에 (partnerA=1, partnerB=2, status=PENDING, inviterUserId=2) 저장
 * 반환값: Pair.of(Optional.of( partnerUserId(=1) ), inviterUsername("user2's name"))
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserConnectionService {

    private final UserService userService;
    private final UserConnectionRepository userConnectionRepository;


    /** [초대 요청을 처리하는 메서드]
     * 초대할 자는 초대받을 자의 userId를 모른다 > 그래서 InviteCode로 진행.
     * 초대한 자(inviterUsrId)가 초대받을 자의 초대코드(inviteCode)를 전달하면 inviteCode의 주인을 찾아
     * 초대 가능한 상태면 PENDING 상태를 DB에 저장하고, 초대받은 자의 UserId와 초대한 자의 이름(username)을 반환한다
     *
     * 반환형: Pair<Optional<Userid>, String>
     * Optional<UserId>: 초대받은 자의 UserId.
     * String: 초대한 자에게 보여줄 메시지 또는 초대한 자의 이름(username)
     */
    public Pair<Optional<UserId>, String> invite(UserId inviterUserId, InviteCode inviteCode) {
        //1. 초대코드(inviteCode)로 파트너(초대 대상) 찾기
        //User = usersId + username
        Optional<User> partner = userService.getUser(inviteCode); //getUser: 초대코드로 username을 찾는 메서드.

        //2. 파트너 없음: 잘못된 초태코드
        if(partner.isEmpty()){
            log.info("Invalid invite code. {}, from {}", inviteCode, inviterUserId);

            return Pair.of(Optional.empty(), "Invalid invite code.");
        }

        //Optional에서 값 추출
        UserId partnerUserId = partner.get().userId();
        String partnerUsername = partner.get().username();

        //3. 자기 자신에게 보낸 초대인지 검사(자기 자신을 초대할 수 없음)
        if(partnerUserId.equals(inviterUserId)){
            return Pair.of(Optional.empty(), "Can't self invite.");
        }

        //4. 현재 두 사람 간 상태 조회(NONE, PENDING, ACCEPTED, DISCONNECTED, REJECTED)
        UserConnectionStatus userConnectionStatus = getStatus(inviterUserId, partnerUserId);

        //5. 상태에 따른 분기 처리
        return switch (userConnectionStatus) {
            case NONE, DISCONNECTED -> {
                //초대자의 이름 가져오기
                //getUsername: userId로 username을 찾는 메서드
                Optional<String> inviterUsername = userService.getUsername(inviterUserId);

                //초대한 사람의 username을 못찾으면 실패 처리(초대자를 못찾은 상태)
                if(inviterUsername.isEmpty()){
                    log.warn("InviteRequest failed.");
                    yield Pair.of(Optional.empty(),"InviteRequest failed.");
                }

                //이름이 비어있지 않다면,
                //사용자가 유효하다는 의미이니까, setStatus로 상태를 변경.
                try{
                    //NONE, DISCONNECTED: 연결이 안된 상태이기에 연결을 요청한 상황 -> PENDING 상태로 저장(초대 요청 등록)
                    //상대방이 수락해야 ACCEPTED가 되는 것임
                    setStatus(inviterUserId, partnerUserId, UserConnectionStatus.PENDING);

                    // 초대한 사람 입장: 초대받을 사람의 userId를 알아야 그 초대받은 사람의 세션을 찾아 알림(webSocket) 전송 가능
                    // 초대받은 사람 입장: 누가 자신을 초대했는지 초대자의 이름을 알아야 함 >> 그래서 inviterUsername이 필요
                    yield Pair.of(Optional.of(partnerUserId), inviterUsername.get());
                }catch (Exception ex){
                    log.error("Set PENDING Failed. cause: {}", ex.getMessage());
                    yield Pair.of(Optional.empty(),"Set PENDING Failed.");
                }
            }
            // 이미 연결(수락)되어 있으면 다시 초대할 필요 없음 -> 사용자에게 알림용 메시지 반환
            case ACCEPTED -> Pair.of(Optional.of(partnerUserId), "Already connected with " + partnerUsername);//이미 연결이 된 상태이기에 에러 메시지 출력 //이미 연결된 상태에서 요청이 들어왔을 때의 상황
            // 이미 초대 중이거나 거절된 상태면 중복 알림 방지
            case PENDING, REJECTED -> {
                log.info("{} invites {} but does not deliver the invitation request.", inviterUserId, partnerUsername);
                yield Pair.of(Optional.of(partnerUserId), "Already invited to " + partnerUsername);  //이미 초대가 된 상황인데 또 보낸(초대 요청을) 상황
            }
        };

    }


    /** [현재 상태 가져오기]
     * 초대/요청 흐름을 처리하기 전에 현재 두 사용자 사이의 상태(이미 초대했는지, 수락됐는지 등)를 판단하기 위해 필요하다
     * 왜 Long.min/Long.max를 사용하는가?
     * - user_connection 테이블은 (partner_a_user_id, partner_b_user_id)를 복합 PK로 사용하고,
     *   (A,B)와 (B,A)를 같은 관계로 보기 위해 서비스 레이어에서 항상 작은 id를 partnerA로,
     *   큰 id를 partnerB로 정규화(canonical ordering)합니다. 따라서 조회 시에도 동일한 규칙 적용.
     */
    private UserConnectionStatus getStatus(UserId inviterUserId, UserId partnerUserId) {
        // repository에서 (partnerA, partnerB)로 찾고, 존재하면 상태 문자열을 enum으로 변환해서 반환
        return userConnectionRepository.findByPartnerAUserIdAndPartnerBUserId(
                Long.min(inviterUserId.id(), partnerUserId.id()),
                Long.max(inviterUserId.id(), partnerUserId.id()))
                   .map(status -> UserConnectionStatus.valueOf(status.getStatus()))
                   .orElse(UserConnectionStatus.NONE);
    }


    /** [현재 상태 갱신하기]
     * 두 유저 간의 상태를 갱신/저장한다.
     */
    @Transactional
    private void setStatus(UserId inviterUserId, UserId partnerUserId, UserConnectionStatus userConnectionStatus){

        //ACCETED로 바꿀 수 없게 방어코드 만들기
        //1000명까지의 remit(제한)이 있다. 여기서 업데이트를 이 Transactional에서는 못막는다. 이 로직을 가지고는 막기 힘들어서 여기서는 ACCEPTED를 튕겨내고 다른 쪽에서 ACCEPTED 처리 할 예정.
        if(userConnectionStatus == UserConnectionStatus.ACCEPTED){
            throw new IllegalArgumentException("Can't set to ACCEPTED.");
        }

        userConnectionRepository.save(new UserConnectionEntity(
                Long.min(inviterUserId.id(), partnerUserId.id()),
                Long.max(inviterUserId.id(), partnerUserId.id()),
                userConnectionStatus, //비꿀 상태값
                inviterUserId.id() //초대자: 초대한 사람
        ));
    }
}
