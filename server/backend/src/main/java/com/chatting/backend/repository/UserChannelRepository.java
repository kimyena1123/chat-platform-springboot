package com.chatting.backend.repository;

import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.projection.ChannelProjection;
import com.chatting.backend.dto.projection.UserIdProjection;
import com.chatting.backend.entity.UserChannelId;
import com.chatting.backend.entity.UserChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    //userId(내 id)로 채팅방 목록 보기
    @Query("SELECT c.channelId AS channelId, c.title AS title, c.headCount AS headCount FROM UserChannelEntity uc " +
            "INNER JOIN ChannelEntity c ON uc.channelId = c.channelId WHERE uc.userId = :userId")
    List<ChannelProjection> findChannelsByUserId(@NonNull @Param("userId") Long userId);


    //채널(채팅방)에 해당 사용자 삭제하기(탈퇴기능)
    void deleteByUserIdAndChannelId(@NonNull Long userId, @NonNull Long channelId);
}
