package com.chatting.backend.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * 해당 테이블에는 복합키가 있다
 * 그래서 해당 복합키를 나타낼 클래스가 따로 필요하다 => UserChannelId
 */
public class UserChannelId implements Serializable {

    private Long userId;
    private Long channelId;

    public UserChannelId() {}

    public UserChannelId(Long userId, Long channelId) {
        this.userId = userId;
        this.channelId = channelId;
    }

    //Getter
    public Long getUserId() {
        return userId;
    }

    public Long getChannelId() {
        return channelId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        UserChannelId userChannelId1 = (UserChannelId) object;
        return Objects.equals(userId, userChannelId1.userId) && Objects.equals(channelId, userChannelId1.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, channelId);
    }

    @Override
    public String toString() {
        return "UserChannelId{userId=%d, channelId=%d}".formatted(userId, channelId);
    }
}
