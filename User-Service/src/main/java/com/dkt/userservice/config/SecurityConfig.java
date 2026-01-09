package com.dkt.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RedisAuthenticationFilter redisAuthenticationFilter;
    private final StatelessJwtFilter statelessJwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/stateless/**").permitAll()
                        .requestMatchers("/api/users/me1").authenticated()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(redisAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(statelessJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}