package com.chatting.messageclient.dto.domain;

/**
 * domain 패키지: 내부 도메인 모델들(불변값 객체들)
 * 목적: 비즈니스 로지겡서 안전하고 명확하게 주고받을 수 있는 값 객체(value objects)를 정의
 *
 * - 불변(immutable)하며, 검증 로직(예. UserId가 양수읹, InviteCode가 비어있지 않은지)을 생성시점에 강제한다.
 *
 * [Message]
 * 목적: 채팅 메시지를 표현하는 아주 단순한 데이터 구조(작성자 username, 본문 content)
 */
public record Message(String username, String content) {}