package com.chatting.backend.repository;

import com.chatting.backend.entity.UserChannelId;
import com.chatting.backend.entity.UserChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface UserChannelRepository extends JpaRepository<UserChannelEntity, UserChannelId> { //복합키이기에 long이 아닌 UserChannelId

    //사용자가 그 해당 채널에 존재하는지 확인
    boolean existsByUserIdAndChannelId(@NonNull Long UserId, @NonNull Long ChannelId);
}
