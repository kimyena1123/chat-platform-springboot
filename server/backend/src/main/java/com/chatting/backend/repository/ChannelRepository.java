package com.chatting.backend.repository;

import com.chatting.backend.dto.projection.ChannelProjection;
import com.chatting.backend.dto.projection.ChannelTitleProjection;
import com.chatting.backend.dto.projection.InviteCodeProjection;
import com.chatting.backend.entity.ChannelEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    //초대코드로 채널id 찾기(초대코드로 채팅방 참여하기 위한 기능을 위해 사용)
    //SELECT channel_id FROM channel WHERE invite_code = ?
    Optional<ChannelProjection> findChannelByInviteCode(@NonNull String inviteCode);

    /**
     * [중요] head_count를 증가/감소시키는 시나리오에서 사용할 "행 잠금" 조회 : headCount를 증가시키거나 감소시킬 때 rock 설정
     *
     * @Lock(PESSIMISTIC_WRITE)
     * - DB가 해당 row에 "배타적 잠금"을 건다(SELECT ... FOR UPDATE)
     * - 같은 row를 수정하려는 다른 트랜잭션은 이 락이 풀릴 때까지 대기(혹은 타임아웃)
     *
     * 왜 필요?
     * - 동시에 여러 사용자가 같은 채널에 join() 하면, 둘 다 head_count를 99→100으로 만들려고 할 수 있다.
     * - 락 없이 처리하면 서로 덮어쓰거나, 101이 되거나, 한계(100)를 넘겨 무너질 수 있다.
     * - 락으로 "한 번에 한 명씩" head_count를 보고/수정하게 만들어 정합성을 보장한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChannelEntity> findForUpdateByChannelId(@NonNull Long channelId);

}
