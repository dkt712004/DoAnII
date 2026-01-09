package com.dkt.userservice.controller;

import com.dkt.userservice.dto.UserProfileDto;
import com.dkt.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/stateless")
@RequiredArgsConstructor
public class StatelessUserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfoStateless(Authentication authentication) {
        try {
            // 1. Logic kiểm tra Token / Session
            // Nếu authentication == null: Tức là RedisAuthenticationFilter không tìm thấy session trong Redis
            // Nguyên nhân: Token không gửi lên, Token sai, hoặc Token ĐÃ BỊ ĐĂNG XUẤT
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Lỗi xác thực: Token không hợp lệ hoặc Phiên đăng nhập đã kết thúc (Đã đăng xuất).");
            }

            // 2. Logic nghiệp vụ (Chỉ chạy khi đã xác thực thành công)
            String username = authentication.getName();
            UserProfileDto userProfile = userService.getUserProfileByUsername(username);

            return ResponseEntity.ok(userProfile);

        } catch (Exception e) {
            // Xử lý các lỗi khác (ví dụ: User không tồn tại trong DB)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Class wrap response để dễ nhìn khi demo
    record SplitBrainResponse(String warning, UserProfileDto data) {}
}