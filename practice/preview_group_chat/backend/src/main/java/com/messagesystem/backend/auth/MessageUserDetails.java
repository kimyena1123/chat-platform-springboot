package com.messagesystem.backend.auth;

import com.fasterxml.jackson.annotation.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Spring Security에서 사용자 정보를 담기 위한 클래스
 * Spring Security의 UserDetails를 구현함으로써, 사용자 인증 관련 정보를 이 객체에 저장하고 인증 처리에 사용함
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private String password;

    //생성자: JSON 요청으로 받을 때 매핑을 위해 @JsonCreator
    @JsonCreator
    public MessageUserDetails(
            @JsonProperty("userId") Long userId,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password) {
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    //로그인 성공 후, 비밀번호를 메모리에서 지움(보안 목적)
    public void erasePassword() {
        password = "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    //권한은 현재는 빈 리스트로 처리(ROLE 기능 없을 때)
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MessageUserDetails that = (MessageUserDetails) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}
