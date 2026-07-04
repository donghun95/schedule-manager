package com.dsa.schedule_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API 테스트 편의를 위해 임시 비활성화
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 11주차 전까지 모든 요청 임시 허용
                );

        return http.build();
    }
}