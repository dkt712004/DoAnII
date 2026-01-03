package com.dkt.authenticationservice.controller;

import com.dkt.authenticationservice.dto.BaseResponse;
import com.dkt.authenticationservice.dto.LoginRequest;
import com.dkt.authenticationservice.dto.LoginResponse;
import com.dkt.authenticationservice.entity.UserEntity;
import com.dkt.authenticationservice.jwt.JwtProvider;
import com.dkt.authenticationservice.service.AuthService;
import com.dkt.authenticationservice.service.InMemoryTokenStore;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;


@RestController
@RequestMapping("/api/auth/v2")
@RequiredArgsConstructor
@Slf4j
public class AuthControllerJwt {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final InMemoryTokenStore tokenStore;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> loginWithJwt(@RequestBody LoginRequest loginRequest) {
        BaseResponse<UserEntity> authResponse = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        if (!"00".equals(authResponse.getCode())) {
            BaseResponse<LoginResponse> errorResponse = new BaseResponse<>(authResponse.getCode(), authResponse.getMessage(), null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        UserEntity authenticatedUser = authResponse.getData();
        String token = jwtProvider.generateToken(authenticatedUser);
        tokenStore.storeToken(token);

        LoginResponse data = new LoginResponse();
        data.setUsername(authenticatedUser.getUsername());
        data.setSessionId(token);

        BaseResponse<LoginResponse> successResponse = new BaseResponse<>("00", "Đăng nhập bằng JWT thành công!", data);
        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<String>> logout(HttpServletRequest request) {
        String token = extractJwtFromRequest(request);

        if (token != null) {
            tokenStore.invalidateToken(token);
        }

        BaseResponse<String> response = new BaseResponse<>("00", "Đăng xuất thành công!", null);
        return ResponseEntity.ok(response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}