package com.chatting.backend.repository;

import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.projection.UserIdProjection;
import com.chatting.backend.entity.UserChannelId;
import com.chatting.backend.entity.UserChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserChannelRepository extends JpaRepository<UserChannelEntity, UserChannelId> { //복합키이기에 long이 아닌 UserChannelId

    //사용자가 그 해당 채널에 존재하는지 참여 여부 확인(특정 사용자가 특정 채널에 속해있는지 확인)
    //SELECT COUNT(*) FROM user_chanel WHERE user_id = ? AND channel_id = ? | SELECT EXISTS (SELECT 1 FROM user_channel WHERE user_id = ? AND channel_id = ?)
    boolean existsByUserIdAndChannelId(@NonNull Long UserId, @NonNull Long ChannelId);

    //특정 채널에 속한 모든 사용자들의 ID를 가져오기(채널 안에 있는 사람들에게 동시에 메시지를 뿌려야 하기 때문)
    //SELECT user_id FROM user_channel WHERE channel_id = ?
    List<UserIdProjection> findUserIdsByChannelId(@NonNull Long channelId);
}
