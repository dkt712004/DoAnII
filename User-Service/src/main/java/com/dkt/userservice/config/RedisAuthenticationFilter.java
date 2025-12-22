package com.dkt.userservice.config;

import com.dkt.userservice.dto.SessionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAuthenticationFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String sessionId = extractToken(request);

        if (StringUtils.hasText(sessionId)) {
            try {
                // LOGIC MỚI: Gọi Redis thay vì giải mã JWT
                // Dùng sessionId làm Key để lấy dữ liệu từ Redis
                Object rawData = redisTemplate.opsForValue().get(sessionId);

                if (rawData != null) {
                    // Convert dữ liệu từ Redis (JSON) sang Object Java
                    SessionData sessionData = objectMapper.convertValue(rawData, SessionData.class);

                    // Tạo danh sách quyền hạn (Roles)
                    List<String> roles = sessionData.getRoles() != null ? sessionData.getRoles() : Collections.emptyList();
                    var authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Tạo đối tượng Authentication
                    UserDetails userDetails = new User(sessionData.getUsername(), "", authorities);
                    var authentication = new UsernamePasswordAuthenticationToken(userDetails, sessionId, authorities);

                    // Lưu SessionData vào Details để Controller dùng
                    authentication.setDetails(sessionData);

                    // Xác thực thành công
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Session ID không tồn tại trong Redis: {}", sessionId);
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception e) {
                log.error("Lỗi khi xác thực Redis: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}