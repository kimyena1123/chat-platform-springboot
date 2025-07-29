package com.messagesystem.backend.auth;

import com.messagesystem.backend.entity.MessageUserEntity;
import com.messagesystem.backend.repository.MessageUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor //생성자 주입 역할
public class MessageUserDetailsService implements UserDetailsService {

    private final MessageUserRepository messageUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //repository에서 user를 찾기
        MessageUserEntity messageUserEntity = messageUserRepository.findByUsername(username).orElseThrow(() -> {
            log.info("User not found: {}", username);

            return new UsernameNotFoundException("");
        });

        return new MessageUserDetails(messageUserEntity.getUserId(), messageUserEntity.getUsername(), messageUserEntity.getPassword());
    }
}
