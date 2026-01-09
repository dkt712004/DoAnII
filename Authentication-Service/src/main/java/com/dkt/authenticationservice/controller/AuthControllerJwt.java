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
import org.springframework.web.bind.annotation.*;

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

    @Value("${server.port}")
    private String serverPort;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> loginWithJwt(@RequestBody LoginRequest loginRequest) {
        BaseResponse<UserEntity> authResponse = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        if (!"00".equals(authResponse.getCode())) {
            BaseResponse<LoginResponse> errorResponse = new BaseResponse<>(authResponse.getCode(), authResponse.getMessage(), null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        log.info("Xử lý Đăng nhập tại Instance chạy port: {}", serverPort);

        UserEntity authenticatedUser = authResponse.getData();
        String token = jwtProvider.generateToken(authenticatedUser);
        tokenStore.storeToken(token);

        LoginResponse data = new LoginResponse();
        data.setUsername(authenticatedUser.getUsername());
        data.setSessionId(token);

        BaseResponse<LoginResponse> successResponse = new BaseResponse<>("00",
                "Đăng nhập thành công tại Auth-Service Port: " + serverPort, data);
        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<String>> logout(HttpServletRequest request) {
        String token = extractJwtFromRequest(request);

        // 1. Kiểm tra nếu không gửi token lên
        if (token == null) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponse<>("AUTH_400", "Vui lòng gửi Token để đăng xuất", null));
        }

        // 2. Gọi hàm xóa và nhận kết quả (True/False)
        boolean isRemoved = tokenStore.invalidateToken(token);

        log.info("Xử lý Đăng xuất tại Instance chạy port: {}", serverPort);

        if (isRemoved) {
            // Trường hợp 1: Tìm thấy token và xóa thành công
            return ResponseEntity.ok(new BaseResponse<>("00",
                    "Đăng xuất thành công tại Auth-Service Port: " + serverPort, null));
        } else {
            // Trường hợp 2: Không tìm thấy token trong RAM (Token rác hoặc Token của Instance khác)
            log.warn("Đăng xuất thất bại tại Port {}: Token không tồn tại trong bộ nhớ.", serverPort);

            // Trả về lỗi để client biết
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>("AUTH_404",
                            "Đăng xuất thất bại! Token không tồn tại (hoặc đang nằm ở Instance khác). Port: " + serverPort, null));
        }
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