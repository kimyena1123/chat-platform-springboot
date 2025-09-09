package com.practice.messagesystem.dto.domain;

/**
 * domain 패키지: 내부 도메인 모델들(불변값 객체들)
 * 목적: 비즈니스 로지겡서 안전하고 명확하게 주고받을 수 있는 값 객체(value objects)를 정의
 *
 * - 불변(immutable)하며, 검증 로직(예. UserId가 양수읹, InviteCode가 비어있지 않은지)을 생성시점에 강제한다.
 *
 * [UserId]
 * 목적: 원시 Long 대신 타입으로 구분되는 ID를 제공. Long 값마 던지는 것보다 더 명확히 하고 실수를 줄임
 * 사용: service/repository/domain에서 유저 식별자로 사용
 */
public record UserId(Long id) {

    public UserId{

        //내부 검증
        //잘못된 ID를 초기에 차단
        if(id == null || id <= 0){
            throw new IllegalArgumentException("Invalid UserId");
        }
    }
}
