package com.chatting.backend.dto.restapi;

/**
 * 로그인 시 전달되는 username, password를 받기 위한 DTO 클래스
 * @param username
 * @param password
 */
public record LoginRequest(String username, String password) {
}
