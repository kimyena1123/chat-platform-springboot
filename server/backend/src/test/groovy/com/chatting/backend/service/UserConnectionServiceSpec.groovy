package com.chatting.backend.service

import com.chatting.backend.constant.UserConnectionStatus
import com.chatting.backend.dto.domain.InviteCode
import com.chatting.backend.dto.domain.User
import com.chatting.backend.dto.domain.UserId
import com.chatting.backend.entity.UserConnectionEntity
import com.chatting.backend.entity.UserEntity
import com.chatting.backend.repository.UserConnectionRepository
import com.chatting.backend.repository.UserRepository
import org.springframework.data.util.Pair
import spock.lang.Specification

/**
 * 주의) 이것때문에 하루를 날렸음!!!
 * 해당 아래 import를 안해도 코드는 별 문제 없이 실행된다. 하지만 에러가 계속 발생한다.
 * 해당 projection을 임포트 안해도 빨간줄이 안쳐지며 임포트 안했다고 알려주지도 않는다.
 * 해당 테스트 코드 등을 다 성공적으로 작성해도 해당 Projection을 import 안하면 코드를 통과하지 못하고 계속 에러 발생.
 */
import com.chatting.backend.dto.projection.UserConnectionStatusProjection
import com.chatting.backend.dto.projection.InviterUserIdProjection

/**
 * 사용자 연결 요청 테스트 코드
 */
class UserConnectionServiceSpec extends Specification {

    UserConnectionService userConnectionService
    UserConnectionLimitService userConnectionLimitService
    UserService userService = Stub()
    UserRepository userRepository = Stub()
    UserConnectionRepository userConnectionRepository = Stub()

    /**
     * - 각 테스트 실행 전 공통적으로 초기화
     * - Service 클래스에 Stub/Repository 주입
     */
    def setup() {
        userConnectionLimitService = new UserConnectionLimitService(userRepository, userConnectionRepository)
        userConnectionService = new UserConnectionService(userService, userConnectionLimitService, userConnectionRepository)
    }

    def "사용자 연결 신청에 대한 테스트."() {
        given: "테스트를 위한 기본 동작 정의 (Stub)"
        // 대상 사용자를 InviteCode로 조회
        userService.getUser(inviteCodeOfTargetUser) >> Optional.of(new User(targetUserId, targetUsername))
        // 연결 신청하는 사람의 ID로 username 조회
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)
        // 연결 신청하는 사람의 연결 수 조회 (limit 테스트용)
        userService.getConnectionCount(senderUserId) >> { senderUserId.id() != 8 ? Optional.of(0) : Optional.of(1_000) }

        // 둘 사이의 현재 상태(연결상태) 가져오기 (projection 사용)
        //예) NONE, PENDING, ACCEPTED, REJECTEC, DISCONNECTED 등 상태만 확인
        //리턴: 실제 DB Entity를 가져오기 않고 "Projection"을 사용해 "상태(ststua)"만 가져온다.
        //사용시점: 수락 전, 거절 전 등 "상태 확인용"으로 주로 사용
        userConnectionRepository.findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }

        when: "실제 invite 동작 호출"
        def result = userConnectionService.invite(senderUserId, usedInviteCode)

        then: "기대 결과 검증"
        result == expectedResult

        where: "테스트 시나리오 정의"
        scenario              | senderUserId  | senderUsername | targetUserId  | targetUsername | inviteCodeOfTargetUser      | usedInviteCode                | beforeConnectionStatus            | expectedResult
        //성공하는 결과를 기대하는 상황
        'Valid invite code'   | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.NONE         | Pair.of(Optional.of(new UserId(2)), 'userA')
        //이미 연결된 상태에서 요청이 들어왔을 때의 상황
        'Already connected'   | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.ACCEPTED     | Pair.of(Optional.empty(), 'Already connected with ' + targetUsername)
        //이미 초대가 된 상황인데 또 보낸(초대 요청을) 상황
        'Already invited'     | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), 'Already invited to ' + targetUsername)
        //거부되었을 때
        'Already rejected'    | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.REJECTED     | Pair.of(Optional.empty(), 'Already invited to ' + targetUsername)
        //연결이 끊겼을 때
        'After disconnected'  | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.of(new UserId(2)), 'userA')
        //잘못된 요청이 들어왔을 때(사용자가 보낸 초대코드와 실제 상대방의 초대코드가 다른 상황) >>  파트너 없음: 잘못된 초태코드
        'Invalid invite code' | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('nobody code') | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), 'Invalid invite code.')
        //스스로 초대하는 상황
        'Self invite'         | new UserId(1) | 'userA'        | new UserId(1) | 'userA'        | new InviteCode('user1code') | new InviteCode('user1code')   | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), "Can't self invite.")
        //limit 도달한 상황
        'Limit reached'       | new UserId(8) | 'userH'        | new UserId(9) | 'userI'        | new InviteCode('user9code') | new InviteCode('user9code')   | UserConnectionStatus.NONE         | Pair.of(Optional.empty(), "Connection limit reached.")
    }

    def "사용자 연결 신청 수락에 대한 테스트."() {

        given:
        userService.getUserId(targetUsername) >> Optional.of(targetUserId)
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)

        // DB에서 신청 수락자 Entity 조회 (연결 한도 테스트용)
        userRepository.findForUpdateByUserId(_ as Long) >> { Long userId ->
            //entity 만들어주기
            def entity = new UserEntity()
            if (userId == 5 || userId == 7) {
                entity.setConnectionCount(1000) // limit 초과 시뮬레이션
            }
            return Optional.of(entity)
        }

        // 두 사용자 간 연결 상태 조회
        //UserConnectionStatus인 연결 Entity를 가져오기 위해 사용
        //예. ACCEPTED 상태인 연결을 찾고, 해당 연결 Entity를 통해 업데이트나 삭제를 수행할 때 사용한다. > Entity 전체를 가져오므로 DB에서 필요한 모든 필드 접근 가능하다
        //사용 시점: 연결을 실제로 수락, 거절, 끊기 할 때. 상태 확인 뿐 아니라 업데이트 작업을 위해 Entity가 필요할 때
        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserIdAndStatus(_ as Long, _ as Long, _ as UserConnectionStatus) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(new UserConnectionEntity(senderUserId.id(), targetUserId.id(), UserConnectionStatus.PENDING, inviter.id()))
            }
        }

        // 둘 사이의 현재 상태(연결상태) 가져오기 (projection 사용)
        //예) NONE, PENDING, ACCEPTED, REJECTEC, DISCONNECTED 등 상태만 확인
        //리턴: 실제 DB Entity를 가져오기 않고 "Projection"을 사용해 "상태(ststua)"만 가져온다.
        //사용시점: 수락 전, 거절 전 등 "상태 확인용"으로 주로 사용
        userConnectionRepository.findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }

        // 초대한 사람의 userId 가져오기
        userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(Stub(InviterUserIdProjection) {
                    getInviterUserId() >> inviter.id()
                })
            }
        }

        when: "accept 동작 수행"
        def result = userConnectionService.accept(senderUserId, targetUsername)

        then:
        result == expectedResult

        where:

        /** [UserId(1)이 UserId(2)에게 초대요청을 함]
         * senderUserId             : 초대요청을 수락하는 사람의 userId
         * senderUsername           : 초대요청을 수락하는 사람의 username
         * targetUserId             : 초대요청을 수락받는 사람의 userId(즉, 초대한 사람)
         * targetUsername           : 초대요청을 수락받는 사람의 username(즉, 초대한 사람)
         * inviterUserId            : 초대요청을 한 사람의 userId
         * beforeConnectionStatus   : 수락받기 전의 둘의 status 관계
         * expectedResult           : 수락하고 나서의 기대되는 결과
         */
        scenario                          | senderUserId  | senderUsername | targetUserId  | targetUsername | inviterUserId              | beforeConnectionStatus            | expectedResult
        //정상적으로 초대가 성공하는 경우(초대요청을 기다리고 있는 PENDING 상태에서 userA가 요청을 수락한 경우)
        'Accept invite'                   | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(Optional.of(new UserId(2)), 'userA')
        //이미 연결된 상황
        'Already connected'               | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.ACCEPTED     | Pair.of(Optional.empty(), 'Already connected.')
        //스스로 수락하는 상황
        'Self accept'                     | new UserId(1) | 'userA'        | new UserId(1) | 'userA'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Can't self accept.")
        //잘못된 초대(초대한 사람이 2번인데 4번한테 수락 완료가 간 상황)
        'Accept wrong invite'             | new UserId(1) | 'userA'        | new UserId(4) | 'userD'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Invalid username.")
        //유효하지 않은 초대
        'Accept invalid invite'           | new UserId(1) | 'userA'        | new UserId(4) | 'userD'        | Optional.empty()           | UserConnectionStatus.NONE         | Pair.of(Optional.empty(), "Invalid username.")
        //수락 거절한 상황
        'After rejected'                  | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.REJECTED     | Pair.of(Optional.empty(), "Accept failed.")
        //수락하고 후에 연결끊는 상황
        'After disconnected'              | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), "Accept failed.")
        //수락 한도(1000명)에 도달했을 때
        'Limit reached'                   | new UserId(5) | 'userE'        | new UserId(6) | 'userF'        | Optional.of(new UserId(6)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Connection limit reached.")
        //수락했더니 다른 사람이 초과한 사람
        'Limit reached by the other user' | new UserId(8) | 'userI'        | new UserId(7) | 'userH'        | Optional.of(new UserId(7)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Connection limit reached by the other user.")
    }


    /**
     * 채팅 요청 거부는 동시성 문제가 없다1
     */
    def "사용자 연결 신청 거절에 대한 테스트."() {

        given:
        userService.getUserId(targetUsername) >> Optional.of(targetUserId)
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)

        // 둘 사이의 현재 상태(연결상태) 가져오기 (projection 사용)
        //예) NONE, PENDING, ACCEPTED, REJECTEC, DISCONNECTED 등 상태만 확인
        //리턴: 실제 DB Entity를 가져오기 않고 "Projection"을 사용해 "상태(ststua)"만 가져온다.
        //사용시점: 수락 전, 거절 전 등 "상태 확인용"으로 주로 사용
        userConnectionRepository.findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }

        // 초대한 사람의 userId 가져오기
        userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(Stub(InviterUserIdProjection) {
                    getInviterUserId() >> inviter.id()
                })
            }
        }

        when:
        def result = userConnectionService.reject(senderUserId, targetUsername)

        then:
        result == expectedResult

        where:

        /** [받은 초대요청을 거절함]
         * senderUserId             : 초대요청 거절하는 사람의 userId
         * senderUsername           : 초대요청 거절하는 사람의 username
         * targetUserId             : 초대요청을 신청한 사람의 userId(즉, 초대한 사람)
         * targetUsername           : 초대요청을 신청한 사람의 username(즉, 초대한 사람)
         * inviterUserId            : 초대요청을 신청한 사람의 userId
         * beforeConnectionStatus   : 거절하기 전 둘의 status 관계
         * expectedResult           : 거절하고 나서 기대되는 결과
         */
        scenario                | senderUserId  | senderUsername | targetUserId  | targetUsername | inviterUserId              | beforeConnectionStatus            | expectedResult
        //정상적으로 초대 요청을 거절하는 상황: userId(1)이 userId(2)의 초대요청을 거절하는 상황
        'Reject invite'         | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(true, 'userB')
        //이미 거절된 상황
        'Already rejected'      | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.REJECTED     | Pair.of(false, 'Reject failed.')
        //스스로의 요청을 reject한 상황
        'Self reject'           | new UserId(1) | 'userA'        | new UserId(1) | 'userA'        | Optional.of(new UserId(1)) | UserConnectionStatus.PENDING      | Pair.of(false, 'Reject failed.')
        //전혀 다른 reject에 대해서 reject하는 상황(2번이 요청한 채팅인데 4번이 요청한 채팅을 거절하는 경우
        'Reject wrong invite'   | new UserId(1) | 'userA'        | new UserId(4) | 'userD'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(false, 'Reject failed.')
        //요청 초대가 없는데 reject하는 상황
        'Reject invalid invite' | new UserId(1) | 'userA'        | new UserId(4) | 'userD'        | Optional.empty()           | UserConnectionStatus.NONE         | Pair.of(false, 'Reject failed.')
        //이미 연결이 끊어진 상황인데 reject하는 상황
        'After disconnect'      | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.DISCONNECTED | Pair.of(false, 'Reject failed.')
    }


    def "사용자 연결 끊기에 대한 테스트."() {
        given:
        userService.getUserId(targetUsername) >> Optional.of(targetUserId)
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)

        // DB에서 신청 수락자 Entity 조회 (연결 한도 테스트용)
        userRepository.findForUpdateByUserId(_ as Long) >> { Long userId ->
            def entity = new UserEntity()
            if (userId != 8) {
                entity.setConnectionCount(100) // limit 관련 시나리오 제외
            }
            return Optional.of(entity)
        }


        // 둘 사이의 현재 상태(연결상태) 가져오기 (projection 사용)
        //예) NONE, PENDING, ACCEPTED, REJECTEC, DISCONNECTED 등 상태만 확인
        //리턴: 실제 DB Entity를 가져오기 않고 "Projection"을 사용해 "상태(ststua)"만 가져온다.
        //사용시점: 수락 전, 거절 전 등 "상태 확인용"으로 주로 사용
        userConnectionRepository.findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }

        // 초대한 사람의 userId 가져오기
        userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(Stub(InviterUserIdProjection) {
                    getInviterUserId() >> inviter.id()
                })
            }
        }

        // 두 사용자 간 연결 상태 조회
        //UserConnectionStatus인 연결 Entity를 가져오기 위해 사용
        //예. ACCEPTED 상태인 연결을 찾고, 해당 연결 Entity를 통해 업데이트나 삭제를 수행할 때 사용한다. > Entity 전체를 가져오므로 DB에서 필요한 모든 필드 접근 가능하다
        //사용 시점: 연결을 실제로 수락, 거절, 끊기 할 때. 상태 확인 뿐 아니라 업데이트 작업을 위해 Entity가 필요할 때
        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserIdAndStatus(_ as Long, _ as Long, _ as UserConnectionStatus) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(new UserConnectionEntity(senderUserId.id(), targetUserId.id(), UserConnectionStatus.ACCEPTED, inviter.id()))
            }
        }

        when:
        def result = userConnectionService.disconnect(senderUserId, targetUsername)

        then:
        result == expectedResult

        where:
        /** [받은 초대요청을 거절함]
         * senderUserId             : 연결 끊는 사람의 userId
         * senderUsername           : 연결 끊는 사람의 username
         * targetUserId             : 연결 끊김 당하는 사람의 userId(즉, 상대방)
         * targetUsername           : 연결 끊김 당하는 사람의 username(즉, 상대방)
         * inviterUserId            : 초대 요청을 신청한 사람의 userId
         * beforeConnectionStatus   : 연결이 끊기기 전 둘의 status 관계
         * expectedResult           : 연결이 끊기고 나서 기대되는 결과
         */
        scenario                | senderUserId  | senderUsername | targetUserId  | targetUsername | inviterUserId              | beforeConnectionStatus            | expectedResult
        //정상적으로 연결 끊기가 성공한 상황: 1번 유저가 2번 유저와 연결 된 상황에서 1번 유저가 연결 끊기
        'Disconnect connection' | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.ACCEPTED     | Pair.of(true, 'userB')
        //둘의 연결상태가 REJECTED인 상황일 때
        'Reject status'         | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.REJECTED     | Pair.of(true, 'userB')
        //둘의 연결상태가 PENDING 상황일 때: 2번 유저가 1번 유저에게 요청을 했는데 수락을 안한 상황(pending)일 때는 실패해야 한다.
        'Pending status'        | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.PENDING      | Pair.of(false, 'Disconnect failed.')
        //이미 연결이 끊겨있는 상황일 때
        'Already disconnected'  | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.DISCONNECTED | Pair.of(false, 'Disconnect failed.')
        //스스로를 연결을 끊을 때
        'Self disconnect'       | new UserId(1) | 'userA'        | new UserId(1) | 'userA'        | Optional.of(new UserId(1)) | UserConnectionStatus.ACCEPTED     | Pair.of(false, 'Disconnect failed.')
        //유저가 정상상황이 아닐 때, 즉 InviterUser에 값이 있어야 하는데 없는 상황일 때
        //그래서 전혀 다른 유저 ID로 들어왔을 때 튕겨내는 상황
        'Disconnect wrong user' | new UserId(1) | 'userA'        | new UserId(1) | 'userA'        | Optional.empty()           | UserConnectionStatus.NONE         | Pair.of(false, 'Disconnect failed.')
        //count가 최소 1이상은 있어야 하는데 조회했을 때 0이 나오면 실패해야 한다.
        'Wrong condition'       | new UserId(8) | 'userH'        | new UserId(9) | 'userI'        | Optional.of(new UserId(9)) | UserConnectionStatus.ACCEPTED     | Pair.of(false, 'Disconnect failed.')
    }
}