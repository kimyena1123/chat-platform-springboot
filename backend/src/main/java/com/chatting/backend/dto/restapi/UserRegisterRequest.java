package com.chatting.backend.dto.restapi;

/**
 * 회원가입시 전달되는 username, password를 받기 위한 DTO 클래스
 */
public record UserRegisterRequest(String username, String password) {
}
