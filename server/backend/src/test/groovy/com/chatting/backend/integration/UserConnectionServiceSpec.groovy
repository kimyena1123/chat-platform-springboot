package com.chatting.backend.integration

import com.chatting.backend.BackendApplication
import com.chatting.backend.constant.UserConnectionStatus
import com.chatting.backend.dto.domain.UserId
import com.chatting.backend.entity.UserConnectionId
import com.chatting.backend.repository.UserConnectionRepository
import com.chatting.backend.repository.UserRepository
import com.chatting.backend.service.UserConnectionLimitService
import com.chatting.backend.service.UserConnectionService
import com.chatting.backend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

/**
 * 테스트 목적: 연결 한도가 10이라고 했을 때, 이미 9개의 연결을 맺은 사용자(A)가
 *              10명이 동시에 수락해도 실제 성공은 1건만 되는지에 대한 테스트
 *
 * [시나리오 요약]
 * 1. 연결 한도(limit)을 10으로 설정한다
 * 2. 테스트 유저 20명(testuser0~testuser19)를 만든다
 * 3. A 사용자(testuser0)에게 이미 9개의 연결을 만들어 놓는다(슬록 1개만 남겨둔다)
 *      - 구현상: 1~9번 유저가 A의 초대코드(inviteCodeA)를 사용해 A에게 초대 > A가 모두 수락
 * 4. A가 10~19번 유저에게 초대를 보낸다(총 10명)
 * 5. 10~19번 유저가 "동시에" 수락한다(스레드로 동시에 accept 호출)
 * 6. 남은 슬롯은 1개뿐이므로 실제 성공(연결 성립)은 정확히 1건이어야 한다(연결된 관계만 ACCEPT이고 나머지 9건은 PENDING이어야 함)
 * 7. 결과 리스트(results)에 담긴 Optional<UserId> 중 isPresent() == true 개수를 세어 1개인지 본다.
 * (동시성 환경에서도 한도 초과가 안나게 해야 한다)
 */

/**
 * "연결 요청 수락"과 "연결 끊기"는 동시성 이슈가 있다.
 */
@SpringBootTest(classes = BackendApplication)
class UserConnectionServiceSpec extends Specification {

    @Autowired
    UserService userService

    @Autowired
    UserConnectionService userConnectionService

    @Autowired
    UserConnectionLimitService userConnectionLimitService

    @Autowired
    UserRepository userRepository

    @Autowired
    UserConnectionRepository userConnectionRepository


    /**
     * 테스트 시나리오 1: "연결 요청 수락은 연결 제한 수를 넘을 수 없다."
     *
     * 목표:
     * - 사용자가 연결 한도(limit)를 초과하지 않도록 동시성 환경에서도 정확하게 처리되는지 검증
     * - A사용자가 이미 9개의 연결을 가진 상태에서 10명의 사용자가 동시에 수락을 시도할 경우
     *   실제 연결 성립은 정확히 1건만 이루어지는지 확인
     */
    def "연결 요청 수락은 연결 제한 수를 넘을 수 없다."() {
        given:
        // (1) 연결 한도를 10으로 설정
        userConnectionLimitService.setLimitConnections(10)

        // (2) 테스트 유저 20명 생성: testuser0 ~ testuser19
        //    userService.addUser(username, password)는 message_user에 한 행을 추가한다.
        (0..19).collect { userService.addUser("testuser${it}", "testpass${it}") }

        // (3) A 사용자 = testuser0 의 userId/초대코드 조회
        //     - getUserId("username"): username으로 UserId를 Optional로 돌려줌
        //     - getInviteCode(userId): 해당 사용자의 초대코드를 Optional로 돌려줌
        def userIdA = userService.getUserId("testuser0").get()
        def inviteCodeA = userService.getInviteCode(userIdA).get()

        // (4) A에게 9개의 연결을 미리 만들어 넣는다.
        //     방식: 1~9번 유저가 "A의 초대코드"를 이용해 A에게 '초대'를 보낸 다음,
        //           A가 그 초대들을 모두 '수락'한다 → 최종 연결 9건 성립.
        (1..9).collect {
            // 1) i번 유저가 A의 코드(inviteCodeA)로 '초대'를 생성 (초대의 대상/주인은 A)
            userConnectionService.invite(userService.getUserId("testuser${it}").get(), inviteCodeA)
            // 2) A가 i번 유저의 초대를 수락 → A의 연결 수가 +1 (최종 9명까지 채움)
            userConnectionService.accept(userIdA, "testuser${it}")
        }

        // (5) A가 10~19번 유저에게 '초대'를 보낼 준비:
        //     각 대상 유저의 초대코드를 모은다(서비스 인터페이스 상 이렇게 설계되어 있음).
        def inviteCodes = (10..19).collect {
            userService.getInviteCode(userService.getUserId("testuser${it}").get()).get()
        }

        // (6) A가 10~19번 유저에게 각각 초대 전송
        //     여기까지 하면 A 입장에서 "보낸 초대"가 10건 대기중(슬롯은 1개 남아있는 상태)
        inviteCodes.each { userConnectionService.invite(userIdA, it) }

        // (7) 수락 결과를 담을 스레드 안전한 리스트 준비
        //     accept()가 (예: Pair<Optional<UserId>, ...>) 형태를 반환한다고 가정하고,
        //     .getFirst() 가 Optional<UserId>라는 가정 하에 isPresent() 여부로 성공/실패 판정.
        def results = Collections.synchronizedList(new ArrayList<Optional<UserId>>())

        when:
        // (8) 10~19번 유저가 "동시에" A의 초대를 수락
        //     - 각 스레드는 자기 userId를 구한 뒤 userConnectionService.accept(userId, "testuser0") 호출
        //     - 성공이면 Optional<UserId>가 present, 실패면 empty일 것으로 가정
        def threads = (10..19).collect { idx ->
            Thread.start {
                def userId = userService.getUserId("testuser${idx}")
                results << userConnectionService.accept(userId.get(), "testuser0").getFirst()
            }
        }

        // (9) 모든 수락 스레드가 끝날 때까지 대기
        threads*.join()

        then:
        // (10) 남은 슬롯은 1개였으므로, 최종 성공 수락(=present)은 정확히 1건이어야 한다.
        //      한도가 제대로 지켜지고(서비스/DB 차원에서 동시성 안전하게) 중복 수락을 막아야 한다는 검증.
        results.count { it.isPresent() } == 1
    }


    /**
     * 테스트 시나리오 2: "연결 요청 카운트는 0보다 작을 수 없다."
     *
     * 목표:
     * - 여러 사용자가 동시에 연결을 끊을 때
     *   연결 카운트가 음수로 내려가는 등의 오류가 발생하지 않는지 검증
     * - 동시에 disconnect 호출 시 동시성 안정성을 테스트
     */
    def "연결 요청 카운트는 0보다 작을 수 없다."() {
        given:

        // 유저 11명 생성: testuser0 ~ testuser10
        (0..10).collect { userService.addUser("testuser${it}", "testpass${it}") }

        // A 사용자 = testuser0의 UserId와 초대코드 조회
        def userIdA = userService.getUserId("testuser0").get()
        def inviteCodeA = userService.getInviteCode(userIdA).get()

        // A에게 10개의 연결 미리 생성
        (1..10).collect {
            // i번 유저가 A의 코드(inviteCodeA)로 '초대'를 생성 (초대의 대상/주인은 A)
            userConnectionService.invite(userService.getUserId("testuser${it}").get(), inviteCodeA)
        }

        // 초대 중 5개는 수락 처리 → 최종 ACCEPTED 5건, 나머지 PENDING 5건
        (1..5).each{
            userConnectionService.accept(userIdA, "testuser${it}")
        }

        // disconnect 결과를 담을 스레드 안전한 리스트
        def results = Collections.synchronizedList(new ArrayList<Boolean>())

        when:
        // 1~10번 유저가 동시에 A와의 연결을 끊는다.
        def threads = (1..10).collect { idx ->
            Thread.start {
                def userId = userService.getUserId("testuser${idx}")

                //모든 사용자들이 testuser0과의 연결을 끊는다.
                results << userConnectionService.disconnect(userId.get(), "testuser0").getFirst()
            }
        }

        // 모든 스레드 종료 대기
        threads*.join()

        then:
        // 실제 성공한 disconnect(=true) 개수는 5건
        //      이미 ACCEPTED 상태였던 5건만 실제로 끊기 처리됨
        results.count { it == true} == 5
        userService.getConnectionCount(userService.getUserId("testuser0").get()).get() == 0


    }

    /**
     * 테스트 종료 후 정리: 생성된 테스트 유저와 연결 정보 삭제
     *
     * 목적:
     * - DB에 테스트 데이터가 남아 있으면 다른 테스트에 영향 가능
     * - 각 UserConnectionStatus 상태별로 삭제 처리
     */
    def cleanup() {
        // 테스트 데이터 정리: 생성한 20명의 사용자 삭제
        //      (주의) 실제 운영 시에는 테스트가 남긴 데이터를 지우는 것이 맞지만,
        //      "메시지 유저도 남겨 두고 싶다"면 이 정리 코드를 제거/수정하면 됨.
        (0..19).each {

            // PENDING인 사용자
            userService.getUserId("testuser${it}").ifPresent { userId ->
                userRepository.deleteById(userId.id())

                userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), UserConnectionStatus.PENDING).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId()),
                            Long.max(userId.id(), it.getUserId())))
                }

                userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), UserConnectionStatus.PENDING).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId()),
                            Long.max(userId.id(), it.getUserId())))
                }
            }

            // ACCEPTED인 사용자
            userService.getUserId("testuser${it}").ifPresent { userId ->
                userRepository.deleteById(userId.id())

                userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), UserConnectionStatus.ACCEPTED).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId()),
                            Long.max(userId.id(), it.getUserId())))
                }

                userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), UserConnectionStatus.ACCEPTED).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId()),
                            Long.max(userId.id(), it.getUserId())))
                }
            }

            // DISCONNECTED인 사용자
            userService.getUserId("testuser${it}").ifPresent { userId ->
                userRepository.deleteById(userId.id())

                userConnectionRepository.findByPartnerAUserIdAndStatus(userId.id(), UserConnectionStatus.DISCONNECTED).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId()),
                            Long.max(userId.id(), it.getUserId())))
                }

                userConnectionRepository.findByPartnerBUserIdAndStatus(userId.id(), UserConnectionStatus.DISCONNECTED).each {
                    userConnectionRepository.deleteById(new UserConnectionId(
                            Long.min(userId.id(), it.getUserId()),
                            Long.max(userId.id(), it.getUserId())))
                }
            }

        }
    }
}
