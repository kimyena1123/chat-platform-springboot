package com.messagesystem.backend.auth;

import com.messagesystem.backend.entity.MessageUserEntity;
import com.messagesystem.backend.repository.MessageUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * DB에서 사용자 정보를 조회하고 UserDetails 객체로 감싸주는 서비스 클래스
 * Spring Security가 내부적으로 사용자 인증 시 호출함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageUserDetailsService implements UserDetailsService {

    private final MessageUserRepository messageUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //username 기준으로 DB에서 사용자 조회
        MessageUserEntity messageUserEntity =
                messageUserRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> {
                                    log.info("User not found: {}", username);
                                    return new UsernameNotFoundException("");
                                });

        //UserDetails 타입으로 반환
        return new MessageUserDetails(
                messageUserEntity.getUserId(),
                messageUserEntity.getUsername(),
                messageUserEntity.getPassword());
    }
}
