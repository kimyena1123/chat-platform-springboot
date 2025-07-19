package com.practice.preview_spring_security_db.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 로그인된 사용자의 인증 상태를 확인하는 컨트롤러
 */
@RestController
@RequestMapping("/api/v1")
public class CheckController {

    @GetMapping("/check")
    public ResponseEntity<String> check(){
        // 현재 사용자의 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 있고, 인증되었는지 여부 확인
        if(authentication != null && authentication.isAuthenticated()){
            return ResponseEntity.ok("인증된 사용자 : " + authentication.getName());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않음");
    }
}
