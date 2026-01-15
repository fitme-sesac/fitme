package com.example.pproject.Config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.pproject.user.entity.UserEntity;            // ★ 추가
import com.example.pproject.user.repository.UserRepository;     // ★ 추가
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    private final UserRepository userRepository; // ★ 주입
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();

        String displayName;
        String email = null;
        if (principal instanceof OAuth2User oauth2User) {
            String provider = "OTHER";
            if (authentication instanceof OAuth2AuthenticationToken oat) {
                provider = oat.getAuthorizedClientRegistrationId(); // google / kakao / ...
            }
            String providerUpper = (provider == null || provider.isBlank())
                    ? "OTHER"
                    : provider.toUpperCase(Locale.ROOT);

            // 소셜 로그인: OAuth2User의 "name" 속성 사용
            displayName = oauth2User.getAttribute("name");
            email = oauth2User.getAttribute("email");

            if (displayName == null || displayName.isBlank()) {
                // 일부 제공자(또는 커스텀 매핑 실패 시)에서 nickname으로 오는 케이스 대비
                displayName = oauth2User.getAttribute("nickname");
            }

            // ✅ 신규 소셜 사용자(DB 미존재)면: 임시 쿠키(OAUTH2_TMP) 발급 후 추가정보 입력 페이지로 이동
            if (email == null || email.isBlank()) {
                response.sendRedirect(frontBaseUrl + "/Login?errorMessage=" + java.net.URLEncoder.encode(
                        "소셜 계정 이메일 정보를 가져오지 못했습니다.", StandardCharsets.UTF_8));
                return;
            }

            Optional<UserEntity> ueByEmail = userRepository.findByEmail(email);
            if (ueByEmail.isEmpty()) {
                Map<String, Object> claims = new HashMap<>();
                claims.put("email", email);
                claims.put("name", displayName);
                claims.put("provider", providerUpper);

                // 10분 유효
                String tmp = jwtTokenProvider.createFlowToken("OAUTH2_REGISTER", claims, 600);
                CookieUtils.addHttpOnlyCookie(request, response, "OAUTH2_TMP", tmp, 600, "Lax");

                // 백엔드 경유(쿠키 검증/쿼리 전달) → 프론트 FirstSocialLogin
                response.sendRedirect("/User/First_Social_Login");
                return;
            }

            // ✅ 기존 소셜 사용자면: DB 기준 정보로 JWT 발급
            UserEntity ue = ueByEmail.get();
            if (ue.getUserid() == null || ue.getUserid().isBlank()) {
                // userid(로그인 아이디)가 비어있으면 내부 식별자 생성(토큰 subject 용)
                String uid = "social_" + UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8))
                        .toString().replace("-", "");
                ue.setUserid(uid);
                userRepository.save(ue);
            }

            displayName = (ue.getUsername() != null && !ue.getUsername().isBlank()) ? ue.getUsername() : displayName;
            Authentication authForToken = new UsernamePasswordAuthenticationToken(
                    ue.getUserid(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + ue.getRoleType().name()))
            );

            String accessToken = jwtTokenProvider.createAccessToken(authForToken, displayName, email);
            CookieUtils.addHttpOnlyCookie(
                    request,
                    response,
                    "ACCESS_TOKEN",
                    accessToken,
                    jwtTokenProvider.getAccessTokenValiditySeconds(),
                    "Lax"
            );

            response.sendRedirect(frontBaseUrl + "/");
            return;
        } else if (principal instanceof UserDetails userDetails) {
            // 일반 로그인: UserDetails.username은 로그인 아이디이므로,
            // DB에서 실제 'username' 필드(실명)를 조회
            String loginId = userDetails.getUsername();
            Optional<UserEntity> ue = userRepository.findByUserid(loginId);
            displayName = ue.map(UserEntity::getUsername).orElse(loginId);
            email = ue.map(UserEntity::getEmail).orElse(null);
        } else {
            displayName = principal.toString();
        }

        if (displayName == null || displayName.isBlank()) {
            displayName = authentication.getName();
        }

        // 일반 로그인: 기존 authentication을 그대로 사용
        Authentication authForToken = authentication;

        // ★ 여기서 JWT 생성 (displayName/email 포함)
        String accessToken = jwtTokenProvider.createAccessToken(authForToken, displayName, email);

        // ★ HttpOnly 쿠키에 저장 (브라우저가 자동 전송)
        CookieUtils.addHttpOnlyCookie(
                request,
                response,
                "ACCESS_TOKEN",
                accessToken,
                jwtTokenProvider.getAccessTokenValiditySeconds(),
                "Lax"
        );

        response.sendRedirect(frontBaseUrl + "/");
    }
}
