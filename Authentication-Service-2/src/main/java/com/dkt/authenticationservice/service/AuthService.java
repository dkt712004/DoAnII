package com.dkt.authenticationservice.service;

import com.dkt.authenticationservice.dto.BaseResponse;
import com.dkt.authenticationservice.dto.LoginRequest;
import com.dkt.authenticationservice.dto.LoginResponse;


import com.dkt.authenticationservice.dto.SessionData;
import com.dkt.authenticationservice.entity.UserEntity;
import com.dkt.authenticationservice.repository.UserEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserEntityRepository userEntityRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate redisTemplate;

    public BaseResponse<UserEntity> authenticate(String username, String rawPassword) {
        BaseResponse<UserEntity> response = new BaseResponse<>();

        Optional<UserEntity> userOpt = userEntityRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            log.warn("Xác thực thất bại: Không tìm thấy user '{}'", username);
            response.setCode("AUTH_404");
            response.setMessage("Tên đăng nhập hoặc mật khẩu không đúng.");
            return response;
        }

        UserEntity user = userOpt.get();

        if (!rawPassword.equals(user.getPassword())) {
            log.warn("Xác thực thất bại: Sai mật khẩu cho user '{}'", username);
            response.setCode("AUTH_401");
            response.setMessage("Tên đăng nhập hoặc mật khẩu không đúng.");
            return response;
        }

        if (!user.isActive()) {
            log.warn("Xác thực thất bại: Tài khoản của user '{}' đã bị vô hiệu hóa.", username);
            response.setCode("AUTH_403");
            response.setMessage("Tài khoản đã bị vô hiệu hóa.");
            return response;
        }


        log.info("Xác thực thành công cho user: {}", username);
        response.setCode("00");
        response.setMessage("Xác thực thành công.");
        response.setData(user);

        return response;
    }

//    public BaseResponse<LoginResponse> login(LoginRequest request) throws Exception {
//        BaseResponse response = new BaseResponse();
//        response.setCode("00");
//        response.setMessage("SUCCESS");
//
//        Optional<UserEntity> userOpt = userEntityRepository.findByUsername(request.getUsername());
//        // Check username exist in db
//        if (userOpt.isEmpty()) {
//            log.info("User not found with username: {}", request.getUsername());
//            response.setCode("USER_404");
//            response.setMessage("User not found!!!");
//            return response;
//        }
//
//        // If user exist -> check password matching
//        UserEntity user = userOpt.get();
//        if (!request.getPassword().equals(user.getPassword())) {
//            log.info("Password not match // Wrong pass");
//            response.setCode("USER_001");
//            response.setMessage("Pass not match");
//            return response;
//        }
//
//        // Setup redis on this project
//
//        // 1. add redis dependency (add to build.gradle)
//        // 2. download and setup redis on machine - get username/pass to connect to redis local
//        // 3. add config for redis (create a bean class to configuration redis dev)
//        // 4. use RedisTemplate cache user
//
//        LoginResponse data = new LoginResponse();
//        data.setUsername(user.getUsername());
//        // 1234123412-12341234-12341234-123412afdf
//        data.setSessionId(String.valueOf(UUID.randomUUID()).replace("-", ""));
//        log.info("user: {}", objectMapper.writeValueAsString(user)); // <- cache this information
//
//        String sessionIdToCache = data.getSessionId();
//        log.info("cache session is ready to save in Redis with key: {}", sessionIdToCache);
//        redisTemplate.opsForValue().set(sessionIdToCache, user);
//        log.info("Cache session success for user: {}", user.getUsername());
//
//        response.setData(data);
//
//        return response;
//    }

    // ... các import ...

    public BaseResponse<LoginResponse> login(LoginRequest request) {
        BaseResponse<LoginResponse> response = new BaseResponse<>();

        // 1. Gọi hàm authenticate và nhận về BaseResponse (Cái hộp)
        BaseResponse<UserEntity> authResult = authenticate(request.getUsername(), request.getPassword());

        // 2. Kiểm tra xem xác thực có thành công không
        // Nếu mã lỗi khác "00", nghĩa là thất bại -> Trả về lỗi ngay
        if (!"00".equals(authResult.getCode())) {
            response.setCode(authResult.getCode());
            response.setMessage(authResult.getMessage());
            return response;
        }

        // 3. Nếu thành công, lấy UserEntity ra khỏi hộp
        UserEntity user = authResult.getData();

        // 4. Tạo Session ID
        String sessionId = UUID.randomUUID().toString();

        // 5. Tạo đối tượng SessionData (Value dạng JSON)
        // Giả sử lấy role mặc định hoặc từ user (nếu bạn đã làm bảng roles)
        List<String> roles = List.of("ROLE_USER");

        SessionData sessionPayload = SessionData.builder()
                .sessionId(sessionId)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName()) // Có thể null nếu chưa cập nhật profile
                .roles(roles)
                .createdAt(System.currentTimeMillis())
                .build();

        // 6. Lưu vào Redis
        try {
            log.info("Lưu session vào Redis: Key={}, Value={}", sessionId, sessionPayload);
            redisTemplate.opsForValue().set(sessionId, sessionPayload, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Lỗi Redis: ", e);
            response.setCode("REDIS_ERROR");
            response.setMessage("Lỗi hệ thống khi lưu phiên đăng nhập.");
            return response;
        }

        // 7. Trả về Session ID cho client
        LoginResponse data = new LoginResponse();
        data.setUsername(user.getUsername());
        data.setSessionId(sessionId);

        response.setCode("00");
        response.setMessage("Đăng nhập thành công!");
        response.setData(data);

        return response;
    }

    public BaseResponse<?> logout(String sessionId) {
        BaseResponse<?> response = new BaseResponse<>();

        if (sessionId == null || sessionId.isEmpty()) {
            response.setCode("400");
            response.setMessage("Session ID không được để trống.");
            return response;
        }

        try {
            Boolean hasKey = redisTemplate.hasKey(sessionId);

            if (Boolean.TRUE.equals(hasKey)) {
                redisTemplate.delete(sessionId);
                log.info("Đã xóa thành công session ID: {}", sessionId);
                response.setCode("00");
                response.setMessage("Đăng xuất thành công!");
            } else {
                log.warn("Không tìm thấy session ID để xóa: {}", sessionId);
                response.setCode("404");
                response.setMessage("Không tìm thấy phiên đăng nhập hợp lệ.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa session khỏi Redis: {}", e.getMessage());
            response.setCode("96");
            response.setMessage("Có lỗi xảy ra trong quá trình đăng xuất.");
        }

        return response;
    }


}
