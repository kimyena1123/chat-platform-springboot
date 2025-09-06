package com.chatting.backend.config;

import com.chatting.backend.auth.RestApiLoginAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

/**
 * Spring Security 관련 설정을 정의한 클래스
 */
@Slf4j
@Configuration
public class SecurityConfig {

    /**
     * 비밀번호를 암호화해주는 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 메모리 기반 유저 정보를 설정
     * 메모리에다가 사용자를 한명만 명시적으로 등록해두고 쓰겠다
     */
//    @Bean
//    public UserDetailsService userDetailsService(){
//        //사용자 생성(ID: testuser / 비밀번호: testpass)
//        UserDetails userDetails = User.builder()
//                .username("testuser")
//                .password(passwordEncoder().encode("testpass"))
//                .roles("USER")
//                .build();
//
//        //메모리에서 사용자 정보를 제공하는 UserDetailsService
//        return new InMemoryUserDetailsManager(userDetails);
//    }

    /**
     * 인증 매니저 설정
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService detailsService, PasswordEncoder passwordEncoder) {
        //위에 메모리 DB처럼 만들었기 떄문에 이걸 사용해줄거다
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();

        daoAuthenticationProvider.setUserDetailsService(detailsService); //사용자 정보 로드
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder); // 비밀번호 검증

        return new ProviderManager(daoAuthenticationProvider); //인증 제공자 등록
    }

    /**
     * Security Filter Chain 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, AuthenticationManager authenticationManager) throws Exception {
        //커스텀 로그인 필터 생성: /api/v1/auth/login이라는 경로에 대해서만 필터 동작
        RestApiLoginAuthFilter restApiLoginAuthFilter = new RestApiLoginAuthFilter(new AntPathRequestMatcher("/api/v1/auth/login", "POST"), authenticationManager);

        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)              // CSRF 비활성화 (API 기반이면 보통 비활성화)
                .httpBasic(AbstractHttpConfigurer::disable)         // 기본 로그인창 비활성화
                .formLogin(AbstractHttpConfigurer::disable)         // form 기반 로그인도 비활성화
                .addFilterAt(restApiLoginAuthFilter, UsernamePasswordAuthenticationFilter.class)    // 커스텀 로그인 필터 등록
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/v1/auth/register",
                                                "/api/v1/auth/login",
                                                "/swagger-ui/**", //swagger
                                                "/v3/api-docs/**",//swagger
                                                "/v3/api-docs.yaml",//swagger
                                                "/swagger-resources/**",//swagger
                                                "/webjars/**").permitAll() // 회원가입, 로그인 요청은 인증 없이 접근 가능
                                        .anyRequest() // 나머지 요청은 인증 필요
                                        .authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")               // 로그아웃 URL 지정
                        .logoutSuccessHandler(this::logoutHandler)      // 로그아웃 성공 핸들러 등록
                );
        return httpSecurity.build(); // 설정 완료 후 필터 체인 반환
    }

    /**
     * 로그아웃 요청 성공/실패 시 실행되는 핸들러
     */
    private void logoutHandler(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        //client에게 줄 응답
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding("UTF-8");

        //메시지를 저장할 변수
        String message;

        if (authentication != null && authentication.isAuthenticated()) {//authentication가 null이 아니고, 인증이 된 상태라면
            response.setStatus(HttpStatus.OK.value());
            message = "Logout success.";
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            message = "Logout failed.";
        }

        try {
            response.getWriter().write(message);
        } catch (IOException ex) {
            log.error("Response failed. cause: {}", ex.getMessage());
        }
    }
}