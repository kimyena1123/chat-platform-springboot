package com.chatting.backend.entity;

import com.chatting.backend.constant.UserConnectionStatus;
import jakarta.persistence.*;

import java.util.Objects;

/**
 * 해당 테이블에는 복합키가 있다
 * 그래서 해당 복합키를 나타낼 클래스가 따로 필요하다 => UserConnectionId
 */
@Entity
@Table(name = "user_connection")
@IdClass(UserConnectionId.class)
public class UserConnectionEntity extends BaseEntity{

    @Id
    @Column(name = "partner_a_user_id", nullable = false)
    private Long partnerAUserId;

    @Id
    @Column(name = "partner_b_user_id", nullable = false)
    private Long partnerBUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserConnectionStatus status;

    @Column(name = "inviter_user_id", nullable = false)
    private Long inviterUserId;

    //생성자
    public UserConnectionEntity() {}

    public UserConnectionEntity(Long partnerAUserId, Long partnerBUserId, UserConnectionStatus status, Long inviterUserId) {
        this.partnerAUserId = partnerAUserId;
        this.partnerBUserId = partnerBUserId;
        this.status = status;
        this.inviterUserId = inviterUserId;
    }

    //getter
    public Long getPartnerAUserId() {
        return partnerAUserId;
    }

    public Long getPartnerBUserId() {
        return partnerBUserId;
    }

    public UserConnectionStatus getStatus() {
        return status;
    }

    public Long getInviterUserId() {
        return inviterUserId;
    }

    //setter
    //setter는 한 개만 필요. 값 변화가 필요한 status만.
    public void setStatus(UserConnectionStatus status) {
        this.status = status;
    }

    //복합키 두 개만 비교하면 된다.
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        UserConnectionEntity that = (UserConnectionEntity) object;
        return Objects.equals(partnerAUserId, that.partnerAUserId) && Objects.equals(partnerBUserId, that.partnerBUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partnerAUserId, partnerBUserId);
    }

    @Override
    public String toString() {
        return "UserConnectionEntity{partnerAUserId=%d, partnerBUserId=%d, status=%s, inviterUserId=%d}"
                .formatted(partnerAUserId, partnerBUserId, status, inviterUserId);
    }
}
