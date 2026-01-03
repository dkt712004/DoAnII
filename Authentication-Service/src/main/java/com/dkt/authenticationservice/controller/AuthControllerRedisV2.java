package com.dkt.authenticationservice.controller;

import com.dkt.authenticationservice.dto.BaseResponse;
import com.dkt.authenticationservice.dto.LoginRequest;
import com.dkt.authenticationservice.service.AuthRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth-redis/v2")
@RequiredArgsConstructor
public class AuthControllerRedisV2 {
    private final AuthRedisService authRedisService;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<String>> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authRedisService.login(loginRequest.getUsername(), loginRequest.getPassword()));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<?>> logout(@RequestBody String sessionId) {
        return ResponseEntity.ok(authRedisService.logout(sessionId));
    }

    @PostMapping("/do-somethings")
    public ResponseEntity<BaseResponse<String>> doSomethings(@RequestBody String sessionId) {
        return ResponseEntity.ok(authRedisService.doSomeThing(sessionId));
    }
}
