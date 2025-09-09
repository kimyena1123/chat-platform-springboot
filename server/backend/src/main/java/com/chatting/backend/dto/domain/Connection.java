package com.chatting.backend.dto.domain;

import com.chatting.backend.constant.UserConnectionStatus;

/** [연결 목록 보기 위한 dto]
 * : 내가 상대방(username)과 어떤 상태(status)인지에 대한 목록을 보기 위한 dto
 */
public record Connection(String username, UserConnectionStatus status) {
}
