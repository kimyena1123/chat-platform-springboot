package com.chatting.backend.service

import com.chatting.backend.constant.UserConnectionStatus
import com.chatting.backend.dto.domain.InviteCode
import com.chatting.backend.dto.domain.User
import com.chatting.backend.dto.domain.UserId
import com.chatting.backend.dto.projection.InviterUserIdProjection
import com.chatting.backend.dto.projection.UserConnectionStatusProjection
import com.chatting.backend.entity.UserConnectionEntity
import com.chatting.backend.entity.UserEntity
import com.chatting.backend.repository.UserConnectionRepository
import com.chatting.backend.repository.UserRepository
import spock.lang.Specification
import org.springframework.data.util.Pair


/**
 * 사용자 연결 요청 테스트 코드
 */
class UserConnectionServiceSpec extends Specification {

    UserConnectionService userConnectionService
    UserConnectionLimitService userConnectionLimitService
    UserService userService = Stub()  //UserService는 테스트 대상이 아니라서 Stub으로 만듦
    UserRepository userRepository = Stub()
    UserConnectionRepository userConnectionRepository = Stub()

    //각 테스트가 실행될 때마다 setup이 실행될테니까
    def setup() {
        userConnectionLimitService = new UserConnectionLimitService(userRepository, userConnectionRepository)
        userConnectionService = new UserConnectionService(userService, userConnectionRepository, userConnectionLimitService)
    }

    def "사용자 연결 신청에 대한 테스트."() {
        given:
        userService.getUser(inviteCodeOfTargetUser) >> Optional.of(new User(targetUserId, targetUsername))
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)
        userService.getConnectionCount(senderUserId) >> {senderUserId.id() != 8 ? Optional.of(0) : Optional.of(1_000)}

        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            //return 값
//            def userConnectionStatusProjection = Stub(UserConnectionStatusProjection)
//            userConnectionStatusProjection.getStatus() >> beforeConnectionStatus.name()

            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }

        when:
        def result = userConnectionService.invite(senderUserId, usedInviteCode)

        then:
        result == expectedResult

        where:
        scenario               | senderUserId  | senderUsername | targetUserId  | targetUsername | inviteCodeOfTargetUser      | usedInviteCode                | beforeConnectionStatus            | expectedResult
        //성공하는 결과를 기대하는 상황
        'Valid invite code'    | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.NONE         | Pair.of(Optional.of(new UserId(2)), 'userA')
        //이미 연결된 상태에서 요청이 들어왔을 때의 상황
        'Already connected'    | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.ACCEPTED     | Pair.of(Optional.of(new UserId(2)), 'Already connected with ' + targetUsername)
        //이미 초대가 된 상황인데 또 보낸(초대 요청을) 상황
        'Already invited'      | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.PENDING      | Pair.of(Optional.of(new UserId(2)), 'Already invited to ' + targetUsername)
        //거부되었을 때
        'Already rejected'     | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.REJECTED     | Pair.of(Optional.of(new UserId(2)), 'Already invited to ' + targetUsername)
        //연결이 끊겼을 때
        'Already disconnected' | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('user2code')   | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.of(new UserId(2)), 'userA')
        //잘못된 요청이 들어왔을 때(사용자가 보낸 초대코드와 실제 상대방의 초대코드가 다른 상황) >>  파트너 없음: 잘못된 초태코드
        'Invalid invite code'  | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | new InviteCode('user2code') | new InviteCode('nobody code') | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), 'Invalid invite code.')
        //스스로 초대하는 상황
        'self invite'          | new UserId(1) | 'userA'        | new UserId(1) | 'userA'        | new InviteCode('user1code') | new InviteCode('user1code')   | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), "Can't self invite.")
        //limit 도달한 상황
        'Limit reached'        | new UserId(8) | 'userH'        | new UserId(9) | 'userI'        | new InviteCode('user9code') | new InviteCode('user9code')   | UserConnectionStatus.NONE         | Pair.of(Optional.empty(), "Connection limit reached.")

    }

    def "사용자 연결 신청 수락에 대한 테스트."() {

        given:
        userService.getUserId(targetUsername) >> Optional.of(targetUserId)
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)
        userRepository.findForUpdateByUserId(_ as Long) >> { Long userId ->
            //entity만들어주기
            def entity = new UserEntity()
            if (userId == 5 || userId == 7) {
                entity.setConnectionCount(1000)
            }
            return Optional.of(entity)
        }

        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserIdAndStatus(_ as Long, _ as Long, _ as UserConnectionStatus) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(new UserConnectionEntity(senderUserId.id(), targetUserId.id(), UserConnectionStatus.PENDING, inviter.id()))
            }
        }

        //둘 사이의 현재 상태 가져오기
        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }

        //초대한 사람의 userId 구하기
        userConnectionRepository.findInviterUserIdByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            inviterUserId.flatMap { UserId inviter ->
                Optional.of(Stub(InviterUserIdProjection) {
                    getInviterUserId() >> inviter.id()
                })
            }
        }

        when:
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
        //수락 거절한상황
        'After rejected'                  | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.REJECTED     | Pair.of(Optional.empty(), "Accept failed.")
        //수락하고 후에 연결끊는 상황
        'After disconnected'              | new UserId(1) | 'userA'        | new UserId(2) | 'userB'        | Optional.of(new UserId(2)) | UserConnectionStatus.DISCONNECTED | Pair.of(Optional.empty(), "Accept failed.")
        //수락 한도(1000명)에 도달했을 때
        'Limit reached'                   | new UserId(5) | 'userE'        | new UserId(6) | 'userF'        | Optional.of(new UserId(6)) | UserConnectionStatus.PENDING      | Pair.of(Optional.empty(), "Connection Limit reached.")
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

        //둘 사이의 현재 상태 가져오기
        userConnectionRepository.findByPartnerAUserIdAndPartnerBUserId(_ as Long, _ as Long) >> {
            Optional.of(Stub(UserConnectionStatusProjection) {
                getStatus() >> beforeConnectionStatus.name()
            })
        }

        //초대한 사람의 Id 구하기
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
}

