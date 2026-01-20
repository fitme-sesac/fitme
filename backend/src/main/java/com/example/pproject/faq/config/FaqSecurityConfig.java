package com.example.pproject.faq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Bean;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class FaqSecurityConfig {

    /**
     * FAQ 전용 보안 설정
     * 개발 환경 기준
     */
    @Bean
    public SecurityFilterChain faqFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 관리자 API
                        .requestMatchers("/api/admin/faq/**").authenticated()
                        // 사용자 조회 API (공개)
                        .requestMatchers("/api/v1/faq/**").permitAll()
                        .anyRequest().permitAll()
                )
                .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer faqWebSecurityCustomizer() {
        return (web) -> web
                .ignoring()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico");
    }
}
