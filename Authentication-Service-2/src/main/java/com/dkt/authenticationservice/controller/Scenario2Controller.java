package com.dkt.authenticationservice.controller;

import com.dkt.authenticationservice.dto.BaseResponse;
import com.dkt.authenticationservice.dto.LoginRequest;
import com.dkt.authenticationservice.dto.LoginResponse;
import com.dkt.authenticationservice.entity.UserEntity;
import com.dkt.authenticationservice.jwt.JwtProvider;
import com.dkt.authenticationservice.service.AuthService;
import com.dkt.authenticationservice.service.BlacklistStoreSc2;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sc2") // Đường dẫn riêng biệt cho Kịch bản 2
@RequiredArgsConstructor
@Slf4j
public class Scenario2Controller {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final BlacklistStoreSc2 blacklistStore; // Sử dụng bộ nhớ RAM cục bộ

    @Value("${server.port}")
    private String serverPort;

    /**
     * 1. API LOGIN
     * - Xác thực user.
     * - Tạo JWT Token.
     * - KHÔNG lưu vào bất kỳ store nào (Stateless mặc định).
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Bước 1: Xác thực username/password
            BaseResponse<UserEntity> authResult = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

            // Nếu xác thực thất bại
            if (!"00".equals(authResult.getCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new BaseResponse<>(authResult.getCode(), authResult.getMessage(), null));
            }

            UserEntity user = authResult.getData();

            // Bước 2: Tạo Token
            String token = jwtProvider.generateToken(user);

            // Bước 3: Trả về Client (Không lưu whitelist gì cả)
            LoginResponse data = new LoginResponse();
            data.setUsername(user.getUsername());
            data.setSessionId(token);

            log.info("SC2: Login thành công tại Port {}", serverPort);

            BaseResponse<LoginResponse> response = new BaseResponse<>("00",
                    "SC2: Đăng nhập thành công tại Auth-Service Port: " + serverPort, data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>("ERR", e.getMessage(), null));
        }
    }

    /**
     * 2. API LOGOUT
     * - Đưa token vào danh sách đen (Blacklist).
     * - Vấn đề: Chỉ đưa vào RAM của Instance hiện tại (ví dụ 8001). Instance kia (8002) không biết.
     */
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<String>> logout(HttpServletRequest request) {
        String token = extractToken(request);

        if (token != null) {
            blacklistStore.addToBlacklist(token);
            log.info("SC2: Đã thêm token vào Blacklist cục bộ tại Port: {}", serverPort);
        }

        BaseResponse<String> response = new BaseResponse<>("00",
                "SC2: Đăng xuất thành công (Token đã bị chặn tại Port " + serverPort + ")", null);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. API KIỂM TRA (CHECK)
     * - Dùng để chứng minh Split Brain.
     * - Nếu gọi vào Port 8001 (nơi đã logout): Sẽ bị Filter chặn -> 403 Forbidden.
     * - Nếu gọi vào Port 8002 (nơi chưa logout): Filter thấy RAM sạch -> Cho qua -> 200 OK (LỖI).
     */
    @GetMapping("/check")
    public ResponseEntity<BaseResponse<String>> checkToken() {
        // Nếu request đi được vào đây, nghĩa là nó đã vượt qua Scenario2Filter
        return ResponseEntity.ok(new BaseResponse<>("00",
                "Token HỢP LỆ tại Port " + serverPort, null));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}