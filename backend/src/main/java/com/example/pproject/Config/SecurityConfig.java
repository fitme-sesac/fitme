package com.example.pproject.Config;

import com.example.pproject.auth.service.CustomOAuth2UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SignedCookieOAuth2AuthorizationRequestRepository signedCookieOAuth2AuthorizationRequestRepository;
    private final NoOpOAuth2AuthorizedClientRepository noOpOAuth2AuthorizedClientRepository;

    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                               PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    static class CsrfCookieExposureFilter implements jakarta.servlet.Filter {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                             jakarta.servlet.ServletResponse response,
                             jakarta.servlet.FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest req = (HttpServletRequest) request;
            CsrfToken csrf = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
            if (csrf != null) {
                // noop
            }
            chain.doFilter(request, response);
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider daoAuthenticationProvider,
                                           OAuth2AuthorizationRequestResolver authorizationRequestResolver) throws Exception {

        http.authenticationProvider(daoAuthenticationProvider);

        // ✅ 완전 무상태: 세션 생성/저장 금지
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.securityContext(sc -> sc.securityContextRepository(
                new org.springframework.security.web.context.NullSecurityContextRepository()));

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();

            // Swagger / OpenAPI
            auth.requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html"
            ).permitAll();

            // Web pages (Thymeleaf) + static
            auth.requestMatchers(
                    "/",
                    "/index",
                    "/Login",
                    "/Logout",
                    "/User/**",
                    "/oauth2/**",
                    "/login/**",
                    "/login/oauth2/**",
                    "/images/**"
            ).permitAll();

            // 토스 페이먼츠 웹훅 (인증 없이 접근 가능해야 함)
            auth.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/payments/webhook").permitAll();

            // 공개 API (인증 없이 접근 가능)
            auth.requestMatchers("/api/public/**").permitAll();
            auth.requestMatchers("/api/auth/**").permitAll();
            auth.requestMatchers("/api/user/find-id/**", "/api/user/find-password/**").permitAll();
            auth.requestMatchers("/api/phone/otp/**").permitAll();

            // 관리자 API
            auth.requestMatchers(
                    "/api/v1/admin/**",
                    "/admin/api/**",
                    "/api/v1/faqs/admin/**",
                    "/api/v1/notices/admin/**"
            ).hasAnyRole("SERVICEADMIN", "APPROVEADMIN", "MASTER");

            // 공개 조회 (READ-ONLY)
            auth.requestMatchers(org.springframework.http.HttpMethod.GET,
                    "/api/v1/faqs/**",
                    "/api/v1/notices/**",
                    "/api/v1/products/**"
            ).permitAll();

            // 광고 노출/클릭 트래킹(비로그인 허용)
            auth.requestMatchers(
                    "/api/v1/ad/serve",
                    "/api/v1/ad/serve/**",
                    "/api/v2/ad/serve",
                    "/api/v2/ad/serve/**"
            ).permitAll();
            auth.requestMatchers(org.springframework.http.HttpMethod.POST,
                    "/api/v1/ad/clicks",
                    "/api/v2/ad/clicks"
            ).permitAll();

            // 광고 캠페인 관리 (기업/관리자)
            auth.requestMatchers("/api/v1/ad/campaigns/**")
                    .hasAnyRole("EMPLOYER", "SERVICEADMIN", "APPROVEADMIN", "MASTER");

            // 기업 전용 API
            auth.requestMatchers(
                    "/api/employer/**",
                    "/api/jobs/**",
                    "/api/talents/**"
            ).hasAnyRole("EMPLOYER", "SERVICEADMIN", "APPROVEADMIN", "MASTER");

            // 커뮤니티: 조회는 공개, 작성/내글은 인증 필요
            auth.requestMatchers(org.springframework.http.HttpMethod.GET,
                    "/api/community/announcements",
                    "/api/community/popular",
                    "/api/community/recommended-members",
                    "/api/community/posts/**"
            ).permitAll();
            auth.requestMatchers("/api/community/my/**").authenticated();
            auth.requestMatchers("/api/community/**").authenticated();

            // 나머지 API는 인증 필요
            auth.requestMatchers("/api/**").authenticated();

            // 그 외(정적/템플릿 등)는 공개
            auth.anyRequest().permitAll();
        });

        http.formLogin(login -> login
                .loginPage("/Login")
                .loginProcessingUrl("/Login")
                .usernameParameter("userid")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .permitAll()
                .successHandler(customAuthenticationSuccessHandler)
                .failureHandler(customAuthenticationFailureHandler)
        );

        http.oauth2Login(oauth2 -> oauth2
                // 프론트에서 /oauth2/authorization/google?prompt=select_account 같은 파라미터를 붙였을 때
                // 실제로 Google 인증 URL로 전달되도록 커스텀 resolver를 연결한다.
                .authorizationEndpoint(ae -> ae
                        .authorizationRequestResolver(authorizationRequestResolver)
                        // ✅ OAuth2 상태값(state/PKCE 등) 세션 대신 서명된 쿠키에 저장
                        .authorizationRequestRepository(signedCookieOAuth2AuthorizationRequestRepository)
                )
                .userInfoEndpoint(user -> user.userService(customOAuth2UserService))
                // ✅ OAuth2AuthorizedClient도 세션에 저장하지 않음
                .authorizedClientRepository(noOpOAuth2AuthorizedClientRepository)
                .successHandler(customAuthenticationSuccessHandler)
                .failureHandler(customAuthenticationFailureHandler)
        );

        http.logout(logout -> logout
                .logoutUrl("/Logout")
                .deleteCookies("JSESSIONID", "ACCESS_TOKEN")
                .logoutSuccessUrl(frontBaseUrl + "/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
        );

        http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                        "/Logout",
                        "/Login",
                        "/User/**",
                        "/api/**",
                        "/oauth2/**",
                        "/login/oauth2/**"
                )
        );

        http.addFilterAfter(new CsrfCookieExposureFilter(), CsrfFilter.class);

        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class);

        http.cors(Customizer.withDefaults());
        http.httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 쿠키 기반 인증을 사용하므로 credentials 허용 + origins를 명시해야 함
        config.setAllowCredentials(true);
        config.setAllowedOrigins(java.util.List.of(
                frontBaseUrl,
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        // 프론트에서 상태 변경 등에 PATCH 사용
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setExposedHeaders(java.util.List.of("Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            org.springframework.security.oauth2.client.registration.ClientRegistrationRepository repo) {

        var defaultResolver = new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(
                repo, "/oauth2/authorization");

        defaultResolver.setAuthorizationRequestCustomizer(customizer -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;

            HttpServletRequest request = attrs.getRequest();
            String prompt = request.getParameter("prompt");
            if (prompt != null && !prompt.isBlank()) {
                customizer.additionalParameters(params -> params.put("prompt", prompt));
            }
        });

        return defaultResolver;
    }
}
