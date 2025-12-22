package com.dkt.authenticationservice.jwt;

import com.dkt.authenticationservice.service.InMemoryTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
public class StatefulJwtAuthFilter extends OncePerRequestFilter {

    private final InMemoryTokenStore tokenStore;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractJwtFromRequest(request);

        if (StringUtils.hasText(token)) {
            if (tokenStore.isTokenValid(token)) {
                try {
                    // Bước 2: KIỂM TRA chữ ký và thời gian hết hạn của token
                    Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
                    String username = claims.getSubject();

                    // Nếu mọi thứ đều hợp lệ, thiết lập Authentication cho Spring Security
                    UserDetails userDetails = new User(username, "", new ArrayList<>());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (Exception e) {
                    // Token có trong kho nhưng không hợp lệ (hết hạn, sai chữ ký...)
                    SecurityContextHolder.clearContext();
                }
            } else {
                // Token không có trong kho (có thể đã logout hoặc không hợp lệ)
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }


    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}