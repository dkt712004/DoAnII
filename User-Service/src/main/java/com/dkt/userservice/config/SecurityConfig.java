package com.dkt.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DynamicFilterConfig dynamicFilterConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("----------------- CustomSecurityConfig -----------------");
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req.anyRequest().permitAll())
                .cors(AbstractHttpConfigurer::disable);
        dynamicFilterConfig.configureFilter(http);
        log.info("----------------- CustomSecurityConfig -----------------");
        return http.build();
    }
}
