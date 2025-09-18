package com.chatting.backend.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "channel")
public class ChannelEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "invite_code", nullable = false)
    private String inviteCode;

    @Column(name = "head_count", nullable = false)
    private int headCount;

    public ChannelEntity() {
    }

    public ChannelEntity(String title, int headCound) {
        this.title = title;
        this.headCount = headCount;
        this.inviteCode = UUID.randomUUID().toString().replace("-", "");
    }

    //Setter
    public void setHeadCound(int headCound) {
        this.headCount = headCount;
    }


    //Getter
    public Long getChannelId() {
        return channelId;
    }

    public String getTitle() {
        return title;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public int getHeadCount() {
        return headCount;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ChannelEntity that = (ChannelEntity) object;
        return Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(channelId);
    }

    @Override
    public String toString() {
        return "ChannelEntity{channelId=%d, title='%s', inviteCode='%s', headCound=%d}"
                .formatted(channelId, title, inviteCode, headCount);
    }
}
