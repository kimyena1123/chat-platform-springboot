package com.chatting.backend.repository;

import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.projection.InviterUserIdProjection;
import com.chatting.backend.dto.projection.UserConnectionStatusProjection;
import com.chatting.backend.dto.projection.UserIdUsernameInviterUserIdProjection;
import com.chatting.backend.entity.UserConnectionEntity;
import com.chatting.backend.entity.UserConnectionId;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnectionEntity, UserConnectionId> {


    //SELECT status FROM user_connection WHERE partner_a_user_id = ? AND partner_b_user_id = ?
    Optional<UserConnectionStatusProjection> findUserConnectionStatusByPartnerAUserIdAndPartnerBUserId(
            @NonNull Long partnerAUserId, @NonNull Long partnerBUserId);


    //SELECT * FROM user_connection WHERE partner_a_user_id = ? AND partner_b_user_id = ? AND status = ?
    Optional<UserConnectionEntity> findByPartnerAUserIdAndPartnerBUserIdAndStatus(
            @NonNull Long partnerAUserId,
            @NonNull Long partnerBUserId,
            @NonNull UserConnectionStatus status);

    //SELECT inviter_user_id FROM user_connection WHERE partner_a_user_id = ? AND partner_b_user_id = ?
    Optional<InviterUserIdProjection> findInviterUserIdByPartnerAUserIdAndPartnerBUserId(
            @NonNull Long partnerAUserId, @NonNull Long partnerBUserId);

    //특정 사용자(A)와 여러 명의 사용자 집합(B 리스트) 사이의 관계를 한번에 카운트 할 때 사용한다.
    //B들 중 몇 명이 A와 특정 상태에 있는가?
    //SELECT COUNT(*) FROM user_connection WHERE partner_a_user_id = ? AND partner_b_user_id IN (?, ?, ?, ?..) AND status = ?
    long countByPartnerAUserIdAndPartnerBUserIdInAndStatus(@NonNull Long partnerAUserId, @NonNull Collection<Long> partnerBUserIds, @NonNull UserConnectionStatus status);
    long countByPartnerBUserIdAndPartnerAUserIdInAndStatus(@NonNull Long partnerBUserId, @NonNull Collection<Long> partnerAUserIds,@NonNull UserConnectionStatus status);

    // ─────────────────────────────────────────────────────────────────────────────
    // 연결 목록(리스트) 조회 쿼리 ①: 내가 partnerA(작은 ID)인 행들
    // ─────────────────────────────────────────────────────────────────────────────
    /**
     * findByPartnerAUserIdAndStatus
     *
     * [무엇을 가져오는가?]
     * - "현재 로그인한 사용자(:userId)가 user_connection 테이블에서 partnerA 로 저장된 모든 행" 중
     *   상태가 :status 인 것들을 조회한다.
     * - 그리고 각 행에서 "반대편 사용자(=partnerB)"의 userId 와 username 을 한 번에 가져오기 위해
     *   UserEntity 와 JOIN 한다.
     *
     * [왜 JOIN 하는가?]
     * - user_connection 에는 상대방의 ID만 있음. 목록 화면에 "상대방 이름"을 띄우려면 user 테이블이 필요.
     * - 따라서 partnerBUserId = userB.userId 로 JOIN 하여 username을 함께 가져온다.
     *
     * [프로젝션 주의]
     * - SELECT 절의 별칭(alias) `AS userId`, `AS username` 은
     *   UserIdUsernameInviterUserIdProjection 의 getter 이름(또는 프로퍼티 이름)과 매칭되어야 한다.
     *
     * [정리]
     * - 내가 A 인 행에서 "상대방(=B)"를 반환 → `partnerBUserId AS userId`, `userB.username AS username`
     */
    @Query(
            "SELECT u.partnerBUserId AS userId, userB.username as username, u.inviterUserId AS inviterUserId "
                    + "FROM UserConnectionEntity u "
                    + "INNER JOIN UserEntity userB ON u.partnerBUserId = userB.userId "
                    + "WHERE u.partnerAUserId = :userId AND u.status = :status")
    List<UserIdUsernameInviterUserIdProjection> findByPartnerAUserIdAndStatus(
            @NonNull @Param("userId") Long userId, @NonNull @Param("status") UserConnectionStatus status);


    // ─────────────────────────────────────────────────────────────────────────────
    // 연결 목록(리스트) 조회 쿼리 ②: 내가 partnerB(큰 ID)인 행들
    // ─────────────────────────────────────────────────────────────────────────────
    /**
     * findByPartnerBUserIdAndStatus
     *
     * [무엇을 가져오는가?]
     * - "현재 로그인한 사용자(:userId)가 user_connection 테이블에서 partnerB 로 저장된 모든 행" 중
     *   상태가 :status 인 것들을 조회한다.
     * - 이 경우 반대편 사용자는 partnerA 이므로, partnerAUserId 를 기준으로 UserEntity 와 JOIN 한다.
     *
     * [왜 두 개의 쿼리가 필요한가?]
     * - user_connection 은 (A,B) 한 쌍으로 저장(정규화)되기 때문에,
     *   동일 사용자 관점에서 보면 어떤 관계는 내가 A 에 있고, 어떤 관계는 내가 B 에 있을 수 있다.
     * - JPA JPQL 은 UNION 을 지원하지 않으므로, A쪽과 B쪽을 각각 조회해서 서비스 계층에서 합치는 방식이 일반적이다.
     *
     * [정리]
     * - 내가 B 인 행에서 "상대방(=A)"를 반환 → `partnerAUserId AS userId`, `userA.username AS username`
     */
    @Query(
            "SELECT u.partnerAUserId AS userId, userA.username as username, u.inviterUserId AS inviterUserId "
                    + "FROM UserConnectionEntity u "
                    + "INNER JOIN UserEntity userA ON u.partnerAUserId = userA.userId "
                    + "WHERE u.partnerBUserId = :userId AND u.status = :status")
    List<UserIdUsernameInviterUserIdProjection> findByPartnerBUserIdAndStatus(
            @NonNull @Param("userId") Long userId, @NonNull @Param("status") UserConnectionStatus status);





}
