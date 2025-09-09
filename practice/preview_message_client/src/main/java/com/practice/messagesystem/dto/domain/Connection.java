package com.practice.messagesystem.dto.domain;

import com.practice.messagesystem.contants.UserConnectionStatus;

/**
 * domain 패키지: 내부 도메인 모델들(불변값 객체들)
 * 목적: 비즈니스 로지겡서 안전하고 명확하게 주고받을 수 있는 값 객체(value objects)를 정의
 *
 * - 불변(immutable)하며, 검증 로직(예. UserId가 양수읹, InviteCode가 비어있지 않은지)을 생성시점에 강제한다.
 *
 * [Connection]
 * 목적: 연결 목록에서 한줄(row)을 표현하는 DTO
 * - 예. username: bob, status: PENDING 같은 항목을 프론트엔드에 본낼 때 사용
 */
public record Connection(String username, UserConnectionStatus status) {}
