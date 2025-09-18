package com.chatting.backend.entity;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * 복합키를 구성하기 위해서는 필수 조건이 있다.
 * 1. @EmbeddedId 또는 @IdClass
 * 2. public의 no-args constructor
 * 3. serializable을 상속 받기4. equals(), hashCode() Override
 */
@Entity
@Table(name = "channel_user")
@IdClass(UserChannelId.class)
public class UserChannelEntity extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "last_read_msg_seq", nullable = false)
    private long lastReadMsgSeq;

    public UserChannelEntity() {
    }

    //채팅방 참여자 Entity
    // 1:1 채팅이라면 : 채팅방 개설자 userId가 추가되어야 하고, 채팅방 참여자의 userId 두 개가 table에 저장되어야 함
    // 그룹 채팅이라면 : 채팅방 개설자 뿐만 아니라 참여자 모두의 userId가 table에 저장되어야 함
    public UserChannelEntity(Long userId, Long channelId, long lastReadMsgSeq) {
        this.userId = userId;                   //채팅방 참여자
        this.channelId = channelId;             //개설할 채팅방 이름
        this.lastReadMsgSeq = lastReadMsgSeq;
    }


    //Setter
    public void setLastReadMsgSeq(long lastReadMsgSeq) {
        this.lastReadMsgSeq = lastReadMsgSeq;
    }


    //Getter
    public Long getUserId() {
        return userId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public long getLastReadMsgSeq() {
        return lastReadMsgSeq;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        UserChannelEntity that = (UserChannelEntity) object;
        return Objects.equals(userId, that.userId) && Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, channelId);
    }

    @Override
    public String toString() {
        return "UserChannelEntity{userId=%d, channelId=%d, lastReadMsgSeq=%d}"
                .formatted(userId, channelId, lastReadMsgSeq);
    }
}
