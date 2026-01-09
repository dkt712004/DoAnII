package com.dkt.authenticationservice.controller; // Sửa lỗi 1

import com.dkt.authenticationservice.dto.BaseResponse;
import com.dkt.authenticationservice.dto.LoginRequest;
import com.dkt.authenticationservice.dto.LoginResponse;
// Sửa lỗi 1 & 2
import com.dkt.authenticationservice.service.AuthService; // Sửa lỗi 1
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthControllerRedis {

    private final AuthService authService;

    @Value("${server.port}")
    private String serverPort;

    /**
     * Flow login: so sánh username và password(có thể dùng thuật toán hash)
     * -> nếu so sánh trùng thì pass -> trả code 00 login success
     * -> không trùng thì cút
     *
     * @param loginRequest
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        BaseResponse<LoginResponse> response = new BaseResponse<>();
        try {
            log.info("herre");
            response = authService.login(loginRequest);
        } catch (Exception e) {
            response.setCode("96");
            response.setMessage("Internal Server Error");
        }
        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<?>> logout(HttpServletRequest request) {
        // Trích xuất session ID từ header "Authorization"
        String sessionId = extractSessionIdFromRequest(request);

        // Gọi service để xử lý logic xóa session
        BaseResponse<?> response = authService.logout(sessionId);

        return ResponseEntity.ok(response);
    }



    /**
     * Hàm helper để lấy session ID từ header "Authorization".
     * Giả định client sẽ gửi theo format "Bearer <sessionId>"
     * @param request
     * @return Session ID hoặc null nếu không có.
     */
    private String extractSessionIdFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Bỏ "Bearer " ở đầu
        }
        return null;
    }
}


// Yeu cau 1
// tao controller auth/v2 dùng jwt de login => token => user service cung dung dc jwt
// api log out => xoa phien cua auth/v2 o auth service nhung user service chua biet la da log out va van su dung phien do ( cac request den user service cung p xac thuc bang jwt)
// Yeu cau 2
// implement reids vao user servcie