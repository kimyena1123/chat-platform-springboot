package com.chatting.backend.entity;


import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "message_user")
public class UserEntity extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;


    //기본생성자
    public UserEntity() {}

    //생성자
    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
    }


    //Getter
    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }


    //username이 unique이기 때문에 username으로 비교
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

    @Override
    public String toString() {
        return "MessageUserEntity{userId=%d, username='%s', createAt=%s, updatedAt=%s}"
                .formatted(userId, username, getCreatedAt(), getUpdatedAt());
    }
}