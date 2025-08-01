package com.messagesystem.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messagesystem.backend.dto.restapi.LoginReqeust;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 사용자 정의 로그인 필터
 * Spring Security에서 로그인 요청을 처리하는 로직을 직접 구현한 것
 * "/api/v1/auth/login"으로 들어온 POST 요청을 처리해서 사용자의 로그인 정보를 인증한다.
 */
public class RestApiLoginAuthFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    //생성자: 로그인 요청 URL과 인증 매니저를 지정
    //AuthenticationManager: 로그인하려면 username/password를 검증해야 한다. 이 검증을 책임지는 객체
    public RestApiLoginAuthFilter(RequestMatcher requiresAuthenticationRequestMatcher, AuthenticationManager authenticationManager) {
        super(requiresAuthenticationRequestMatcher, authenticationManager);
    }


    /**
     * 인증 시도 로직. 로그인 요청이 들어왔을 때 실행되는 메서드
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)throws AuthenticationException, IOException {

        //JSON 타입이 아니면 로그인 시도 거부(Content-Type이 application/json인지 검사)
        if (!request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {throw new AuthenticationServiceException(
                    "Unsupported Content-Type: " + request.getContentType());
        }

        //요청 바디에서 로그인 요청 객체 추출(JSON으로 들어온 요청 바디를 LoginRequest 객체로 파싱)
        LoginReqeust loginRequest = objectMapper.readValue(request.getInputStream(), LoginReqeust.class);

        //인증 토큰 생성(id/password 전달): 사용자 아이디와 비밀번호를 담은 인증 토큰 생성
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());

        //인증 매니저에게 인증 위임(DaoAuthenticationProvider 사용 예정)
        return getAuthenticationManager().authenticate(authenticationToken);
    }

    /**
     * 인증 성공 시 실행되는 메서드
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {

        //인증 정보를 SecurityContext에 저장
        SecurityContext securityContext = SecurityContextHolder.getContext();
        ((MessageUserDetails) authResult.getPrincipal()).erasePassword();
        securityContext.setAuthentication(authResult);

        //이 인증 정보를세션에도 젖아(세션 기반 인증을 위한 작업)
        HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
        contextRepository.saveContext(securityContext, request, response);

        // 세션 ID를 Base64로 인코딩하여 응답으로 반환
        String sessionId = request.getSession().getId();
        String encodedSessionId = Base64.getEncoder().encodeToString(sessionId.getBytes(StandardCharsets.UTF_8));

        //클라이언트에게 응답(세션 ID 반환):
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(encodedSessionId);
        response.getWriter().flush();
    }

    /**
     * 인증 실패 시 실행되는 메서드
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {

        //실패했을 때는 저장할 게 없어서 응답만 만들어서 client에게 준다
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write("Not authenticated.");
        response.getWriter().flush();
    }
}
