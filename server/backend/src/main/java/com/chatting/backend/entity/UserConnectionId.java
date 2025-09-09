package com.chatting.backend.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * 해당 테이블에는 복합키가 있다
 * 그래서 해당 복합키를 나타낼 클래스가 따로 필요하다 => UserConnectionId
 */
public class UserConnectionId implements Serializable {

    private Long partnerAUserId;
    private Long partnerBUserId;

    public UserConnectionId() {
    }

    public UserConnectionId(Long partnerAUserId, Long partnerBUserId) {
        this.partnerAUserId = partnerAUserId;
        this.partnerBUserId = partnerBUserId;
    }

    //getter
    public Long getPartnerAUserId() {
        return partnerAUserId;
    }

    public Long getPartnerBUserId() {
        return partnerBUserId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        UserConnectionId that = (UserConnectionId) object;
        return Objects.equals(partnerAUserId, that.partnerAUserId) && Objects.equals(partnerBUserId, that.partnerBUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partnerAUserId, partnerBUserId);
    }

    @Override
    public String toString() {
        return "UserConnectionId{partnerAUserId=%d, partnerBUserId=%d}"
                .formatted(partnerAUserId, partnerBUserId);
    }
}
