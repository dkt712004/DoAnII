package com.dkt.authenticationservice.config;

import com.dkt.authenticationservice.jwt.Scenario2Filter;
import com.dkt.authenticationservice.jwt.StatefulJwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final Scenario2Filter scenario2Filter;
    private final StatefulJwtAuthFilter statefulJwtAuthFilter;
    private final RedisSessionAuthFilter redisSessionAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/sc2/login").permitAll() // Cho phép login kịch bản 2
                        .requestMatchers("/api/auth/v1/check", "/api/auth/v1/logout").authenticated()
                        .requestMatchers("/api/sc2/**").authenticated()
                        .requestMatchers("/api/users/me1").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/auth-redis/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(scenario2Filter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(redisSessionAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(statefulJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
}
