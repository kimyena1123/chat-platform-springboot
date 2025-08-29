package com.chatting.backend.service;

import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.entity.UserConnectionEntity;
import com.chatting.backend.entity.UserEntity;
import com.chatting.backend.repository.UserConnectionRepository;
import com.chatting.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class UserConnectionLimitService {

    private final UserRepository userRepository;
    private final UserConnectionRepository userConnectionRepository;

    // 한 사용자가 가질 수 있는 "최대 연결 수" 제한.
    // 테스트에서 값을 바꿔가며 검증하기 위해 final 상수가 아닌 일반 필드로 한다.
    @Getter
    @Setter
    private int limitConnections = 1_000;

//    //그래서 이 limit 제한을 getter, setter로 만들어서 테스트에서 변경하면서 쓸 수 있도록 할 것이다.
//    public int getLimitConnections() {
//        return limitConnections;
//    }
//
//    public void setLimitConnections(int limitConnections) {
//        this.limitConnections = limitConnections;
//    }

    /**
     * - "초대 요청을 받은 사용자"가 수락할 때 호출됩니다.
     * - 현재 두 사용자 사이의 관계가 PENDING(초대 대기) 상태인지 확인하고,
     *   두 사용자 모두 "연결 수 제한(limitConnections)"을 넘지 않았는지 검사한 뒤,
     *   정상이라면 둘의 connectionCount를 +1 하고, 관계 상태를 ACCEPTED로 변경합니다.
     *
     * [상세 흐름]
     * 1) 두 사용자 ID를 "정규화"해서 (작은 ID = partnerA, 큰 ID = partnerB)로 고정
     *    - user_connection 테이블의 PK가 (partner_a_user_id, partner_b_user_id)로 구성되었다고 가정
     *    - (A,B)와 (B,A)를 동일한 관계로 본다는 뜻 → 항상 같은 순서로 조회/저장!
     * 2) 두 사용자 행(UserEntity)을 '쓰기 잠금'으로 조회
     *    - repository의 findForUpdate... 를 통해 DB에서 SELECT ... FOR UPDATE 실행 (비관적 락)
     *    - 동시에 여러 스레드가 수락을 눌러도 connectionCount가 꼬이지 않도록 보호
     * 3) 두 사용자 사이의 관계(UserConnectionEntity)가 "PENDING"인지 확인 (없으면 예외)
     * 4) 두 사용자 각각의 connectionCount가 limitConnections에 도달했는지 검사
     *    - 하나라도 제한에 걸리면 수락 불가(에러)
     * 5) 둘 다 OK면 두 사용자 connectionCount를 +1, 관계 상태를 ACCEPTED로 변경
     *
     * @param acceptorUserId  초대 요청을 수락하는 사람(= 지금 이 메서드를 호출한 주체)
     * @param inviterUserId   초대를 보낸 사람
     */
    // @Transactional 덕분에 메서드가 정상 종료되면 커밋되며,
    // 위에서 잡은 DB 락도 해제된다
    @Transactional
    public void accept(UserId acceptorUserId, UserId inviterUserId) {
        // (1) 두 사용자 ID를 "정규화"해서 항상 같은 순서로 다룸
        //     - 이렇게 해야 같은 두 사람 간의 관계를 항상 동일한 키로 조회/갱신 가능
        Long firstUserId = Long.min(acceptorUserId.id(), inviterUserId.id());
        Long secondUserId = Long.max(acceptorUserId.id(), inviterUserId.id());

        // (2) 두 사용자 정보를 '쓰기 잠금'으로 조회
        //     - findForUpdateByUserId는 JPA의 PESSIMISTIC_WRITE(SELECT ... FOR UPDATE)를 사용하게끔 설계되어 있어야 함
        //     - 이유: 동시에 여러 요청이 들어와 connectionCount가 꼬이거나 limit를 초과 저장하는 상황을 방지
        UserEntity firstUserEntity = userRepository.findForUpdateByUserId(firstUserId)
                .orElseThrow(() -> new EntityNotFoundException("Invalid userId: " + firstUserId));
        UserEntity secondUserEntity = userRepository.findForUpdateByUserId(secondUserId)
                .orElseThrow(() -> new EntityNotFoundException("Invalid userId: " + secondUserId));

        // (3) 두 사용자 사이의 현재 관계가 "PENDING(대기)"인지 확인
        //     - 초대를 보냈고 아직 수락/거절이 확정되지 않은 상태여야만 수락 가능
        //SELECT * FROM user_connection WHERE partner_a_userId = ? AND partner_b_userId = ? AND status = ?
        UserConnectionEntity userConnectionEntity
                = userConnectionRepository.findByPartnerAUserIdAndPartnerBUserIdAndStatus(firstUserId, secondUserId, UserConnectionStatus.PENDING)
                    .orElseThrow(() -> new EntityNotFoundException("Invalid status."));


        // 에러를 구분해서 던져주기 어렵다. 왜? firstUserId에 뭐가 들어갈 지 모르는 상황이고 secondUserId에 뭐가 들어갈지 모르는 상황이기 때문이다. 이걸 찾는 함수가 필요하다.
        // (4) 예외 메시지 생성기(람다) 준비
        //     - 어떤 사용자가 limit를 넘었는지에 따라 에러 문구를 다르게 보여주기 위함
        //     - java.util.function.Function<T, R>는 "입력 1개 → 출력 1개" 형태의 함수형 인터페이스
        //     - 여기서는 (userId) -> (그 userId가 수락자면 메시지 A, 아니면 메시지 B)
        Function<Long, String> getErrorMessage =
                userId -> userId.equals(acceptorUserId.id()) ? "Connection Limit reached." : "Connection limit reached by the other user.";

        // (5) 각 사용자 현재 연결 수 확인
        int firstConnectionCount = firstUserEntity.getConnectionCount();
        int secondConnectionCount = secondUserEntity.getConnectionCount();

        // (6) 연결 수 제한 검사
        //     - "현재 연결 수 >= 최대 허용 수"이면 더 이상 새로운 연결을 만들 수 없음
        //     - >= 를 쓰는 이유: 예) limitConnections=1000, 현재 1000이면 이미 한도에 도달 → 추가 불가
        if(firstConnectionCount >= limitConnections) {
            //// firstUserId가 한도에 도달했으므로 수락 불가
            throw new IllegalStateException(getErrorMessage.apply(firstUserId));
        }
        if(secondConnectionCount >= limitConnections) {
            //수락 불가
            throw new IllegalStateException(getErrorMessage.apply(secondUserId));
        }

        // 업데이트하기
        // (7) 둘 다 OK면 실제 반영
        //     - 관계는 양방향 의미이므로 두 사용자 모두 connectionCount +1
        firstUserEntity.setConnectionCount(firstConnectionCount + 1);
        secondUserEntity.setConnectionCount(secondConnectionCount + 1);

        //     - 관계 상태를 ACCEPTED로 바꿔서 "연결 완료"로 확정
        userConnectionEntity.setStatus(UserConnectionStatus.ACCEPTED);

    }

}
