package com.practice.preview_spring_security_db.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.preview_spring_security_db.restapi.LoginReqeust;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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


public class RestApiLoginAuthFilter extends AbstractAuthenticationProcessingFilter {

    //JSON 파싱을 위한 ObjectMapper
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
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {

        //Content-Type이 application/json인지 검사
        MediaType mediaType = MediaType.parseMediaType(request.getContentType());
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            throw new AuthenticationServiceException("지원하지 않는 타입: " + mediaType);
        }

        //JSON으로 들어온 요청 바디를 LoginRequest 객체로 파싱
        LoginReqeust loginRequest = objectMapper.readValue(request.getInputStream(), LoginReqeust.class);

        //사용자 아이디와 비밀번호를 담은 인증 토큰 생성
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
        securityContext.setAuthentication(authResult);

        //이 인증 정보를세션에도 젖아(세션 기반 인증을 위한 작업)
        HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
        contextRepository.saveContext(securityContext, request, response);

        //클라이언트에게 응답(세션 ID 반환)
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write(request.getSession().getId());//session id를 바로 준다
        response.getWriter().flush();
    }

    /**
     * 인증 실패 시 실행되는 메서드
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        //실패했을 때는 저장할 게 없어서 응답만 만들어서 client에게 준다
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write("인증 실패");
        response.getWriter().flush();
    }

}
