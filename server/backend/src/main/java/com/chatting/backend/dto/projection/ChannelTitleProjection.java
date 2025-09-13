package com.chatting.backend.dto.projection;

// =================================================================================================
// 역할: JPA Projection 인터페이스 - 채널 제목만 부분 조회할 때 사용.
// 장점: 엔티티 전체를 읽지 않고 필요한 컬럼만 select → 성능/네트워크 최적화.
// 사용 예:
//   Optional<String> title = channelRepository.findChannelTitleByChannelId(id).map(ChannelTitleProjection::getTitle);
// =================================================================================================

public interface ChannelTitleProjection {

    String getTitle();
}
