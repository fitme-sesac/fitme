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
public class NoticeSecurityConfig { // 👈 이 부분이 빠져있었을 가능성이 큽니다!

    // NoticeWebMvcConfig에서 등록한 CorsConfigurationSource 빈을 주입받거나,
    // 메서드 호출을 위해 필요하다면 여기서 주입받을 수도 있습니다.
    // 하지만 보통 http.cors(cors -> cors.configurationSource(corsConfigurationSource())) 형태로
    // 빈 이름을 찾아서 자동으로 매핑되기도 합니다.
    // 만약 에러가 난다면 아래 주석을 풀고 필드 주입을 받으세요.
    /*
    private final CorsConfigurationSource corsConfigurationSource;
    public NoticeSecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }
    */

    /**
     * Notice 전용 보안 설정
     */
    @Bean
    public SecurityFilterChain noticeFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // CORS 설정: 스프링 컨테이너에 등록된 corsConfigurationSource 빈을 사용
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 관리자 전용 API
                        .requestMatchers("/api/admin/notices/**", "/api/admin/policies/**").hasRole("ADMIN")

                        // 2. 사용자(Public) 조회 API
                        .requestMatchers("/api/v1/notices/**").permitAll()

                        // 3. 그 외 모든 요청은 인증 필요 (순서 중요!)
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