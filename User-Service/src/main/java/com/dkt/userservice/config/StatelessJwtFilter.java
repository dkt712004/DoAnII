package com.dkt.userservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatelessJwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);
            if (token != null) {
                // 1. Giải mã Token (Chỉ check chữ ký, KHÔNG check Redis)
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();

                if (username != null) {
                    // 2. Tạo Authentication và nạp vào Context
                    // Đây là bước giúp Controller có thể dùng biến authentication
                    UserDetails userDetails = new User(username, "", new ArrayList<>());
                    var auth = new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("Stateless Auth Success for: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Stateless Auth Failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    // Cấu hình để Filter này CHỈ CHẠY cho đường dẫn stateless
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Nếu URL KHÔNG bắt đầu bằng /api/users/stateless thì bỏ qua filter này
        return !request.getServletPath().startsWith("/api/users/stateless");
    }



    private String extractToken(HttpServletRequest request) {
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