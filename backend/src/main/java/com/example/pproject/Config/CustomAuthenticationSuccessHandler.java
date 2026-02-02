package com.example.pproject.Config;

import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    private final UserRepository userRepository;
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
                provider = oat.getAuthorizedClientRegistrationId(); // google / kakao / naver
            }
            String providerUpper = (provider == null || provider.isBlank())
                    ? "OTHER"
                    : provider.toUpperCase(Locale.ROOT);

            email = oauth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                response.sendRedirect(frontBaseUrl + "/Login?errorMessage=" + java.net.URLEncoder.encode(
                        "소셜 계정 이메일 정보를 가져오지 못했습니다.", StandardCharsets.UTF_8));
                return;
            }

            Optional<UserEntity> ueByEmail = userRepository.findByEmail(email);

            // ✅ 신규 소셜 사용자: 임시 쿠키(OAUTH2_TMP) 발급 → 추가정보 페이지로
            if (ueByEmail.isEmpty()) {
                Map<String, Object> claims = new HashMap<>();
                claims.put("email", email);
                claims.put("provider", providerUpper);

                // ✅ 이름(name)만 (닉네임 X)
                putIfPresent(claims, "name", oauth2User.getAttribute("name"));

                // ✅ 프리필 통일 키: gender/birthday/phone
                putIfPresent(claims, "gender", oauth2User.getAttribute("gender")); // MALE/FEMALE/UNDISCLOSED
                putIfPresent(claims, "birthday", oauth2User.getAttribute("birthday")); // YYYY-MM-DD
                putIfPresent(claims, "phone", oauth2User.getAttribute("phone")); // digits

                String tmp = jwtTokenProvider.createFlowToken("OAUTH2_REGISTER", claims, 600);
                CookieUtils.addHttpOnlyCookie(request, response, "OAUTH2_TMP", tmp, 600, "Lax");

                response.sendRedirect("/User/First_Social_Login");
                return;
            }

            // ✅ 기존 소셜 사용자면: DB 기준 정보로 JWT 발급
            UserEntity ue = ueByEmail.get();
            if (ue.getUserid() == null || ue.getUserid().isBlank()) {
                String uid = "social_" + UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8))
                        .toString().replace("-", "");
                ue.setUserid(uid);
                userRepository.save(ue);
            }

            displayName = (ue.getUsername() != null && !ue.getUsername().isBlank())
                    ? ue.getUsername()
                    : oauth2User.getAttribute("name");

            java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + ue.getRoleType().name()));
            JwtUserPrincipal jwtPrincipal = new JwtUserPrincipal(
                    ue.getId(), ue.getUserid(), displayName, email, authorities);
            Authentication authForToken = new UsernamePasswordAuthenticationToken(
                    jwtPrincipal, null, jwtPrincipal.getAuthorities());

            String accessToken = jwtTokenProvider.createAccessToken(authForToken, displayName, email);
            CookieUtils.addHttpOnlyCookie(
                    request,
                    response,
                    "ACCESS_TOKEN",
                    accessToken,
                    jwtTokenProvider.getAccessTokenValiditySeconds(),
                    "Lax");

            response.sendRedirect(frontBaseUrl + "/");
            return;
        }

        // 일반 로그인
        Optional<UserEntity> ueOpt = Optional.empty();
        if (principal instanceof UserDetails userDetails) {
            String loginId = userDetails.getUsername();
            ueOpt = userRepository.findByUserid(loginId);
            displayName = ueOpt.map(UserEntity::getUsername).orElse(loginId);
            email = ueOpt.map(UserEntity::getEmail).orElse(null);
        } else {
            displayName = principal.toString();
        }

        if (displayName == null || displayName.isBlank()) {
            displayName = authentication.getName();
        }

        // JWT에 회원 DB id(member_id) 포함: 이력서/지원 등 API에서 user.getId() 사용
        String accessToken;
        if (ueOpt.isPresent()) {
            UserEntity ue = ueOpt.get();
            JwtUserPrincipal jwtPrincipal = new JwtUserPrincipal(
                    ue.getId(), ue.getUserid(), displayName, email, authentication.getAuthorities());
            Authentication authForToken = new UsernamePasswordAuthenticationToken(
                    jwtPrincipal, null, jwtPrincipal.getAuthorities());
            accessToken = jwtTokenProvider.createAccessToken(authForToken, displayName, email);
        } else {
            accessToken = jwtTokenProvider.createAccessToken(authentication, displayName, email);
        }
        CookieUtils.addHttpOnlyCookie(
                request,
                response,
                "ACCESS_TOKEN",
                accessToken,
                jwtTokenProvider.getAccessTokenValiditySeconds(),
                "Lax");

        response.sendRedirect(frontBaseUrl + "/");
    }

    private void putIfPresent(Map<String, Object> out, String key, Object value) {
        if (value == null)
            return;
        String s = String.valueOf(value).trim();
        if (s.isBlank())
            return;
        out.put(key, s);
    }
}
