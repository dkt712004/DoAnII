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
public class RedisSessionAuthFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Chỉ bỏ qua các đường dẫn của kịch bản lỗi
        boolean skip = path.startsWith("/api/sc2") || path.startsWith("/api/users/stateless");
        if (skip) log.info(">>> REDIS FILTER: Bỏ qua đường dẫn: {}", path);
        return skip;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info(">>> REDIS FILTER: Bắt đầu kiểm tra request: {}", request.getServletPath());
        String sessionId = extractSessionId(request);

        if (sessionId != null) {
            log.info(">>> REDIS FILTER: Tìm thấy SessionID: {}", sessionId);
            try {
                // 1. Lấy dữ liệu từ Redis
                Object rawData = redisTemplate.opsForValue().get(sessionId);

                if (rawData != null) {
                    log.info(">>> REDIS FILTER: Tìm thấy dữ liệu trong Redis!");

                    // 2. Convert dữ liệu
                    SessionData sessionData = objectMapper.convertValue(rawData, SessionData.class);
                    log.info(">>> REDIS FILTER: Convert thành công user: {}", sessionData.getUsername());

                    // 3. Tạo quyền
                    List<String> roles = sessionData.getRoles() != null ? sessionData.getRoles() : Collections.emptyList();
                    var authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // 4. Set Authentication
                    User principal = new User(sessionData.getUsername(), "", authorities);
                    var auth = new UsernamePasswordAuthenticationToken(principal, sessionId, authorities);
                    auth.setDetails(sessionData);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info(">>> REDIS FILTER: ✅ Xác thực thành công!");
                } else {
                    log.warn(">>> REDIS FILTER: ❌ Không tìm thấy dữ liệu trong Redis (Token sai hoặc hết hạn)");
                }
            } catch (Exception e) {
                log.error(">>> REDIS FILTER: 🔥 LỖI EXCEPTION: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else {
            log.warn(">>> REDIS FILTER: ⚠️ Không tìm thấy Token trong Header");
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