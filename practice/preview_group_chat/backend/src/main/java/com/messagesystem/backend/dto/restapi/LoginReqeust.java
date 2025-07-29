package com.messagesystem.backend.dto.restapi;

/**
 * 로그인 시 전달되는 username, password를 받기 위한 DTO 클래스
 */
public record LoginReqeust(String username, String password) {
}
