package com.example.pproject.notice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class NoticeSecurityConfig {

    // ❌ 삭제됨: corsConfigurationSource 빈 생성 코드를 지웠습니다.
    // (NoticeWebMvcConfig에서 이미 생성한 것을 가져다 씁니다)

    @Bean
    public SecurityFilterChain noticeFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        // 위 파라미터(corsConfigurationSource)는 스프링이 알아서
        // NoticeWebMvcConfig에 있는 빈을 찾아서 넣어줍니다.

        http
                .csrf(csrf -> csrf.disable()) // 테스트를 위해 CSRF 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 관리자 전용 API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 2. 사용자(Public) 조회 API
                        .requestMatchers("/api/v1/notices/**", "/api/v1/faqs/**").permitAll()

                        // 3. 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer noticeWebSecurityCustomizer() {
        return (web) -> web
                .ignoring()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico");
    }
}