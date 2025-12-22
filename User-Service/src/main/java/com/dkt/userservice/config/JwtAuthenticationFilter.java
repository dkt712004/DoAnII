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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractJwtFromRequest(request);

        // Chỉ xử lý khi có token. Nếu không có token thì bỏ qua (không throw exception)
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                if (Objects.isNull(claims)) {
                    // Log lỗi để debug nhưng không throw RuntimeException ra ngoài controller
                    log.warn("Claims is null");
                    return;
                }

                String username = claims.getSubject();
                if (Objects.isNull(username)) {
                    log.warn("Username is null in token");
                    return;
                }

                log.info("Authenticated username: {}", username);

                // Nếu token hợp lệ, thiết lập authentication
                UserDetails userDetails = new org.springframework.security.core.userdetails.User(username, "", new ArrayList<>());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // Token không hợp lệ (hết hạn, sai chữ ký...) -> Xóa Context để đảm bảo an toàn
                log.error("Invalid JWT Token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // QUAN TRỌNG: Đã xóa khối else { throw new RuntimeException(); }
        // Luôn luôn cho phép request đi tiếp để các filter phía sau xử lý
        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
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