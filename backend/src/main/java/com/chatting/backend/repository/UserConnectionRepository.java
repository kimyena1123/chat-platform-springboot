package com.chatting.backend.repository;

import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.projection.InviterUserIdProjection;
import com.chatting.backend.dto.projection.UserConnectionStatusProjection;
import com.chatting.backend.entity.UserConnectionEntity;
import com.chatting.backend.entity.UserConnectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnectionEntity, UserConnectionId> {

    //Optional<UserConnectionStatusProjection>로 하면
    // select * from ...가 아니라 select status from ...와 같은 의미이다.
    //SELECT status FROM user_connection WHERE partner_a_user_id = ? AND partner_b_user_id = ?
    Optional<UserConnectionStatusProjection> findByPartnerAUserIdAndPartnerBUserId(
            @NonNull Long partnerAUserId, @NonNull Long partnerBUserId);


    //SELECT * FROM user_connection WHERE partner_a_user_id = ? AND partner_b_user_id = ? AND status = ?
    Optional<UserConnectionEntity> findByPartnerAUserIdAndPartnerBUserIdAndStatus(
            @NonNull Long partnerAUserId,
            @NonNull Long partnerBUserId,
            @NonNull UserConnectionStatus status);

    //SELECT inviter_user_id FROM user_connection WHERE partner_a_user_id = ? AND partner_b_user_id = ?
    Optional<InviterUserIdProjection> findInviterUserIdByPartnerAUserIdAndPartnerBUserId(
            @NonNull Long partnerAUserId, @NonNull Long partnerBUserId);


}
