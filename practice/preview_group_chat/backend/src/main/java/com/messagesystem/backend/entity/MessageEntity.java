package com.messagesystem.backend.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "message")
public class MessageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_sequence")
    private Long messageSequence;

    @Column(name = "user_name", nullable = false)
    private String username;

    @Column(name = "content", nullable = false)
    private String content;


    //기본생성자
    public MessageEntity() {}

    //생성자
    public MessageEntity(String username, String content) {
        this.username = username;
        this.content = content;
    }


    //Getter
    public Long getMessageSequence() {
        return messageSequence;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }



    //두 객체의 내용(값)을 비교하기 위해 필요
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        MessageEntity that = (MessageEntity) object;
        return Objects.equals(messageSequence, that.messageSequence);
    }

    //HashMap, MashSet 등에 들어갈 때 동등 객체 판단에 필수
    @Override
    public int hashCode() {
        return Objects.hashCode(messageSequence);
    }

    //toString
    @Override
    public String toString() {
        return "MessageEntity{messageSequence=%d, username='%s', content='%s', createdAt=%s, updatedAt=%s}"
                .formatted(messageSequence, username, content, getCreatedAt(), getUpdatedAt());
    }
}
