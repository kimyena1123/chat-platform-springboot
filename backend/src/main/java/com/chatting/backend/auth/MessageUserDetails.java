package com.chatting.backend.auth;

import com.fasterxml.jackson.annotation.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * [UserDetails]: MessageUserEntity를 Spring Security가 사용 가능한 형태로 만들어주는 어댑터 역할을 한다.
 * Spring Security에서 인증된 사용자 정보를 담는 클래스
 */
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@Class")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private String password;

    //Getter

    /**
     * 역직렬화 시 필요한 생성자
     * @JsonCreator와 @JsonProperty 덕분에 Jackson이 JSON → 객체로 변환할 수 있음
     */
    @JsonCreator
    public MessageUserDetails(
            @JsonProperty("userId") Long userId,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password)
    {
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

    //Getter
    public Long getUserId() {
        return userId;
    }

    // Security가 사용자 이름을 가져올 때 사용 (ID처럼 사용됨)
    @Override
    public String getUsername() {
        return username;
    }

    // Security가 DB에서 가져온 암호화된 비밀번호를 비교할 때 사용
    @Override
    public String getPassword() {
        return password;
    }

    // 로그인 이후 비밀번호 정보를 지우고 싶을 때 호출
    public void erasePassword(){
        password = "";
    }

    /**
     * 권한 정보(roles, authorities)를 반환하는 메서드
     * 우리는 현재 권한을 사용하지 않기 때문에 빈 리스트 반환
     */
    @Override
    @JsonIgnore //꼭 해야 함!! 안하면 websocket 연결안됨. 해당 필드를 JSON 직렬화(serialization) 또는 역직렬화(deserialization) 대상에서 제외하라는 뜻이다.
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    /**
     * 객체 비교를 위한 equals 구현
     * username이 유일하다고 가정하여 이를 기준으로 비교
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        MessageUserDetails that = (MessageUserDetails) object;
        return Objects.equals(username, that.username);
    }

    // HashSet, HashMap 등에서 사용할 해시코드
    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }


    /**
     * 현재 사용 안함.
     * 메서드 설명
     */
/*// 계정 만료 여부 - true면 사용 가능
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정 잠김 여부 - true면 사용 가능
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    // 자격증명(비밀번호) 만료 여부 - true면 사용 가능
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정 활성화 여부 - true면 사용 가능
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
    */
}
