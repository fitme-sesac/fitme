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

    private final SignedCookieOAuth2AuthorizationRequestRepository signedCookieAuthRequestRepo;
    private final NoOpOAuth2AuthorizedClientRepository noOpOAuth2AuthorizedClientRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;

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

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/test1", "/test2").permitAll();

            auth.requestMatchers(
                    "/Login",
                    "/Logout",
                    "/User/Login",
                    "/User/Register",
                    "/User/Find_Userid",
                    "/User/Verify_Userid_Code",
                    "/User/Find_Password",
                    "/User/Find_Password_Link",
                    "/User/Password_Reset_Link",
                    "/User/Verify_Code",
                    "/User/New_Password",
                    "/User/Change_Password",
                    "/oauth2/**",
                    "/login/**"
            ).permitAll();

            auth.requestMatchers(
                    "/user/update", "/board/new", "/board/edit", "/board/delete",
                    "/board/like", "/comment/insert", "/user/info", "/User/MyPage",
                    "/user/other_user_page", "/user/change_password", "/user/re_enter_credentials"
            ).authenticated();

            auth.requestMatchers("/User/Register", "/user/find-userid", "/user/find-password").anonymous();
            auth.requestMatchers("/images/**").permitAll();

            auth.requestMatchers(
                    "/product/new", "/product/edit", "/product/delete",
                    "/qna/insert", "/qna/delete/{id}", "/qna/update/{id}",
                    "/document/insert", "/document/update/", "/plantation/delete",
                    "/plantation/insert", "/plantation/update",
                    "/event/delete", "/event/create", "/event/update",
                    "/admin/**", "/fruit/create", "/fruit/delete", "/fruit/update",
                    "/disease/create", "/disease/delete", "/disease/update"
            ).hasAnyRole("SERVICEADMIN","APPROVEADMIN","MASTER");

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
                        .authorizationRequestRepository(signedCookieAuthRequestRepo)
                )
                .authorizedClientRepository(noOpOAuth2AuthorizedClientRepository)
                .userInfoEndpoint(user -> user.userService(customOAuth2UserService))
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
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
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
