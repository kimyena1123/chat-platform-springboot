package com.chatting.backend.dto.domain;

import java.util.Objects;

/** 채널 도메인 DTO (엔티티가 아닌, 읽기 전용 뷰/전달 모델) */
public record Channel(ChannelId channelId, String title, int headCount) { // Channel이 가지고 있어야 할 정보는 UserChannelId, title, headCound

    /** 채널 동등성은 channelId만으로 판단 (title/headCount 변경과 무관) */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Channel channel = (Channel) object;
        return Objects.equals(channelId, channel.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(channelId);
    }
}
