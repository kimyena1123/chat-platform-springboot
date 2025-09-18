package com.chatting.backend.repository;

import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.projection.ChannelTitleProjection;
import com.chatting.backend.dto.projection.InviteCodeProjection;
import com.chatting.backend.entity.ChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<ChannelEntity, Long> {

    //channelId로 채널명(title) 찾기
    //SELECT title FROM channel WHERE channelId = ?
    Optional<ChannelTitleProjection> findChannelTitleByChannelId(@NonNull Long channelId);

    //채팅방의 초대코드를 찾기
    //SELECT channel_invite_code FROM channel WHERE channel_id = ?
    Optional<InviteCodeProjection> findChannelInviteCodeByChannelId(@NonNull Long channelId);
}
