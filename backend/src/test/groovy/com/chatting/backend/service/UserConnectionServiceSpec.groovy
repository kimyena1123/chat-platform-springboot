package com.chatting.backend.service

import com.chatting.backend.constant.UserConnectionStatus
import com.chatting.backend.dto.domain.InviteCode
import com.chatting.backend.dto.domain.User
import com.chatting.backend.dto.domain.UserId
import com.chatting.backend.dto.projection.UserConnectionStatusProjection
import com.chatting.backend.repository.UserConnectionRepository
import spock.lang.Specification
import org.springframework.data.util.Pair

/**
 * 사용자 연결 요청 테스트 코드
 */
class UserConnectionServiceSpec extends Specification {

    UserConnectionService userConnectionService
    UserService userService = Stub() //UserService는 테스트 대상이 아니라서 Stub으로 만듦
    UserConnectionRepository userConnectionRepository = Stub()
    //UserConnectionService가 사용되면서 이 내부에서 db를 사용할텐데 단위 테스트니까 목킹하겠다

    //각 테스트가 실행될 때마다 setup이 실행될테니까
    def setup() {
        userConnectionService = new UserConnectionService(userService, userConnectionRepository)
    }

    def "사용자 연결 신청에 대한 테스트."() {
        given:
        userService.getUser(inviteCodeOfTargetUser) >> Optional.of(new User(targetUserId, targetUsername))
        userService.getUsername(senderUserId) >> Optional.of(senderUsername)
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
    }
}
