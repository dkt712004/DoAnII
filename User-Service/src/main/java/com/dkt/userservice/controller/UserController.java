package com.dkt.userservice.controller;

import com.dkt.userservice.dto.SessionData;
import com.dkt.userservice.dto.UserProfileDto;
import com.dkt.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me1")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            UserProfileDto userProfile = userService.getUserProfileByUsername(username);
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
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