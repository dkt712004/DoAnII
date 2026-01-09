package com.dkt.authenticationservice.config;

import com.dkt.authenticationservice.dto.SessionData;
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
public class RedisSessionAuthFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String sessionId = extractSessionId(request);

        if (sessionId != null) {
            try {
                // 1. Lấy dữ liệu từ Redis
                Object rawData = redisTemplate.opsForValue().get(sessionId);

                if (rawData != null) {
                    // 2. Convert dữ liệu lấy được thành SessionData
                    SessionData sessionData = objectMapper.convertValue(rawData, SessionData.class);

                    // 3. Tạo quyền (Authorities) - Giả sử user có role mặc định nếu trong session không có
                    List<String> roles = sessionData.getRoles() != null ? sessionData.getRoles() : Collections.emptyList();
                    var authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // 4. Thiết lập Authentication cho Spring Security
                    User principal = new User(sessionData.getUsername(), "", authorities);
                    var auth = new UsernamePasswordAuthenticationToken(principal, sessionId, authorities);

                    // Lưu SessionData vào details để controller có thể lấy dùng
                    auth.setDetails(sessionData);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.error("Lỗi xác thực Redis Session: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractSessionId(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}