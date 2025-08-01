package com.messagesystem.backend.controller;


import com.messagesystem.backend.dto.restapi.UserRegisterRequest;
import com.messagesystem.backend.service.MessageUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@SuppressWarnings("unused")
public class MessageUserController {

    private final MessageUserService messageUserService;

    public MessageUserController(MessageUserService messageUserService) {
        this.messageUserService = messageUserService;
    }

    @PostMapping("/register") //localhost:8080/api/v1/auth/register
    public ResponseEntity<String> register(@RequestBody UserRegisterRequest request) {
        try {
            messageUserService.addUser(request.username(), request.password());
            return ResponseEntity.ok("User registered.");
        } catch (Exception ex) {
            log.error("Failed to add user. cause: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to register.");
        }
    }

    @PostMapping("/unregister")//localhost:8080/api/v1/auth/unregister
    public ResponseEntity<String> unregister(HttpServletRequest request) {
        try {
            messageUserService.removeUser();
            request.getSession().invalidate();
            return ResponseEntity.ok("User unregistered.");
        } catch (Exception ex) {
            log.error("Failed to remove user. cause: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to unregister user.");
        }
    }
}
