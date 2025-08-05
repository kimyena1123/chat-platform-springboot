package com.chatting.backend.auth;

import com.chatting.backend.entity.MessageUserEntity;
import com.chatting.backend.repository.MessageUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * [UserDetailsService]: ]pring Security와 DB를 연결하는 브릿지 역할
 * Spring Security에서 사용자 인증을 위해 사용하는 서비스 클래스
 * UserDetailsService를 구현해야 Security가 로그인 시 사용자 정보를 가져올 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor // final 필드를 생성자로 자동 주입
public class MessageUserDetailsService implements UserDetailsService {

    private final MessageUserRepository messageUserRepository; //해당 repository에는 username을 조회하는 메서드가 있다.

    /**
     * Spring Security가 로그인 시 사용자의 username으로 DB에서 사용자 정보를 조회하기 위해 호출하는 메서드
     * @param username 로그인 폼에서 사용자가 입력한 ID
     * @return UserDetails: 인증에 사용할 사용자 정보 객체
     * @throws UsernameNotFoundException 사용자를 찾지 못했을 때 발생
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //repository에서 user를 찾기:DB에서 username에 해당하는 사용자 정보 조회
        MessageUserEntity messageUserEntity = messageUserRepository.findByUsername(username).orElseThrow(() -> {
            log.info("User not found: {}", username);

            return new UsernameNotFoundException(""); // 사용자를 못 찾으면 예외 발생 → Spring Security가 인증 실패 처리함
        });

        //MessageUserDetails는 Spring Security가 사용가능한 형태로 만들어주는 어댑터 역할으 한다. 아래 매개변수들을 Spring Security가 사용할 수 있도록 MessageUserDetailsfh 보낸다.
        // 조회한 사용자 정보를 Spring Security가 사용할 수 있는 형태(UserDetails)로 감싸서 반환
        return new MessageUserDetails(messageUserEntity.getUserId(), messageUserEntity.getUsername(), messageUserEntity.getPassword());
    }
}
