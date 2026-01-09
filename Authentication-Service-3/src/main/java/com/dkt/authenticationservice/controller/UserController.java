package com.dkt.authenticationservice.controller;

import com.dkt.authenticationservice.dto.BaseResponse;
import com.dkt.authenticationservice.dto.SessionData;
import com.dkt.authenticationservice.dto.UserProfileDto;
import com.dkt.authenticationservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/me1")
    public ResponseEntity<?> getMyInfo(Authentication authentication) {
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

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo() {
        // Lấy authentication từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Lấy SessionData đã lưu ở bước Filter
        SessionData sessionData = (SessionData) authentication.getDetails();

        return ResponseEntity.ok(sessionData); // Trả về chính JSON đã lưu trong Redis
    }

}