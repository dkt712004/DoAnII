package com.dkt.authenticationservice.jwt;

import com.dkt.authenticationservice.service.BlacklistStoreSc2;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class Scenario2Filter extends OncePerRequestFilter {

    private final BlacklistStoreSc2 blacklistStore;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("--- [SC2 FILTER] Bắt đầu kiểm tra request tại Port: {}", request.getLocalPort());
        String token = extractToken(request);

        if (token != null) {
            log.info("[SC2 FILTER] Đã tìm thấy Token: {}...", token.substring(0, 10));

            // 1. Check Blacklist
            if (blacklistStore.isBlacklisted(token)) {
                log.error("[SC2 FILTER] ❌ THẤT BẠI: Token nằm trong Blacklist!");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token Revoked");
                return;
            } else {
                log.info("[SC2 FILTER] ✅ Blacklist check OK (Token không nằm trong Blacklist)");
            }

            // 2. Giải mã JWT
            try {
                log.info("[SC2 FILTER] Đang giải mã JWT...");
                Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
                String username = claims.getSubject();

                log.info("[SC2 FILTER] ✅ Giải mã thành công! User: {}", username);

                UserDetails userDetails = new User(username, "", new ArrayList<>());
                var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("[SC2 FILTER] ✅ Đã set Authentication vào Context");

            } catch (Exception e) {
                log.error("[SC2 FILTER] ❌ LỖI GIẢI MÃ TOKEN: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else {
            log.warn("[SC2 FILTER] Không tìm thấy Token trong header");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean skip = !request.getServletPath().startsWith("/api/sc2");
        if (!skip) {
            log.info("Request vào đường dẫn SC2: {}", request.getServletPath());
        }
        return skip;
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