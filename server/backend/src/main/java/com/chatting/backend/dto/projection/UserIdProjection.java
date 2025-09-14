package com.chatting.backend.dto.projection;

/**
 * username만 가져오기
 * projection이란, DB에서 엔티티 전체가 아니라 특정 필드만 가져오게 해주는 JPA 기능
 */
public interface UserIdProjection {

    Long getUserId();
}
