package com.chatting.backend.repository;

import com.chatting.backend.constant.UserConnectionStatus;
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
    Optional<UserConnectionStatusProjection> findByPartnerAUserIdAndPartnerBUserId(
            @NonNull Long partnerAUserId, @NonNull Long partnerBUserId);


    Optional<UserConnectionEntity> findByPartnerAUserIdAndPartnerBUserIdAndStatus(
            @NonNull Long partnerAUserId,
            @NonNull Long partnerBUserId,
            @NonNull UserConnectionStatus status);

    Optional<InviterUserIdProjection> findInviterUserIdByPartnerAUserIdAndPartnerBUserId(
            @NonNull Long partnerAUserId, @NonNull Long partnerBUserId);

}
