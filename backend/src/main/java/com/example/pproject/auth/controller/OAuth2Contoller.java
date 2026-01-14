package com.example.pproject.auth.controller;

import com.example.pproject.Config.CookieUtils;
import com.example.pproject.Config.JwtTokenProvider;
import com.example.pproject.Constant.SocialType;
import com.example.pproject.user.controller.PhoneOtpController;
import com.example.pproject.user.dto.UserRequestDTO;
import com.example.pproject.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OAuth2Contoller {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 프론트 주소 (redirect용)
     * application.yml / yml 에서 설정
     */
    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    /**
     * (기존 기능 유지)
     * - OAUTH2_TMP 쿠키(JWT)에 담아둔 email/username 등을 꺼내서 화면(또는 프론트 라우트)에 넘김
     */
    @GetMapping("/oauth2/register")
    public String socialRegisterForm(Model model,
                                     @CookieValue(value = "OAUTH2_TMP", required = false) String tmpToken) {

        // tmpToken이 없으면 홈/로그인 등으로 보내는 게 안전 (원래 정책에 맞게 수정 가능)
        if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
            return "redirect:" + frontBaseUrl + "/Login";
        }

        Claims claims = jwtTokenProvider.getClaims(tmpToken);

        Map<String, Object> flowClaims = flowClaimsOf(claims);
        String email = flowClaims.get("email") == null ? null : flowClaims.get("email").toString();
        String username = flowClaims.get("name") == null ? null : flowClaims.get("name").toString();

        UserRequestDTO userDTO = new UserRequestDTO();
        userDTO.setEmail(email);
        userDTO.setUsername(username);

        // (기존처럼 화면에서 data 바인딩 쓰는 로직 유지)
        model.addAttribute("data", userDTO);

        // ✅ "기존 기능 해치지 않기" 관점:
        // - 백엔드에서 Thymeleaf 화면을 계속 쓸거면 아래 그대로 유지
        // return "User/SocialRegister";

        // ✅ 네 요구(백엔드 프론트 삭제) 관점:
        // - templates를 삭제했다면 Thymeleaf 반환하면 500 남 -> 프론트 라우트로 redirect 해야 함
        String q = "email=" + enc(email) + "&username=" + enc(username);
        return "redirect:" + frontBaseUrl + "/FirstSocialLogin?" + q;
    }

    /**
     * (기존 기능 유지)
     * - 추가정보 저장 후 OAUTH2_TMP 쿠키 제거
     */
    @PostMapping("/oauth2/register")
    public String socialRegister(UserRequestDTO userDTO,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 @CookieValue(value = "OAUTH2_TMP", required = false) String tmpToken,
                                 @CookieValue(value = "PHONE_VERIFIED_TMP", required = false) String phoneVerifiedToken) {

        if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
            return "redirect:" + frontBaseUrl + "/Login?errorMessage=" + enc("소셜 가입 절차가 만료되었습니다. 다시 로그인해주세요.");
        }
        Claims c = jwtTokenProvider.getClaims(tmpToken);
        if (!"OAUTH2_REGISTER".equals(c.get("flowType", String.class))) {
            return "redirect:" + frontBaseUrl + "/Login?errorMessage=" + enc("소셜 가입 절차가 만료되었습니다. 다시 로그인해주세요.");
        }
        Map<String, Object> flow = flowClaimsOf(c);
        String emailFromOAuth = flow.get("email") == null ? null : flow.get("email").toString();
        if (emailFromOAuth == null || userDTO.getEmail() == null || !emailFromOAuth.equalsIgnoreCase(userDTO.getEmail())) {
            return "redirect:" + frontBaseUrl + "/Login?errorMessage=" + enc("이메일 정보가 일치하지 않습니다. 다시 로그인해주세요.");
        }

        // 휴대폰 인증(쿠키 기반)
        String normalizedPhone = userDTO.getPhone() == null ? "" : userDTO.getPhone().replaceAll("[^0-9]", "");
        String verifiedPhone = PhoneOtpController.readVerifiedPhone(jwtTokenProvider, phoneVerifiedToken, "SIGNUP");
        if (verifiedPhone == null || !verifiedPhone.equals(normalizedPhone)) {
            throw new IllegalStateException("휴대폰 인증을 완료해주세요.");
        }

        userDTO.setSocialType(SocialType.GOOGLE); // 기존 코드 유지
        userDTO.setPhone(normalizedPhone);
        userDTO.setPhoneVerifiedAt(java.time.LocalDateTime.now());
        userDTO.setTermsAgreedAt(java.time.LocalDateTime.now());
        userDTO.setPrivacyAgreedAt(java.time.LocalDateTime.now());
        userDTO.setPolicyAgreedAt(java.time.LocalDateTime.now());
        if (Boolean.TRUE.equals(userDTO.getMarketingOptIn())) {
            userDTO.setMarketingAgreedAt(java.time.LocalDateTime.now());
        } else {
            userDTO.setMarketingOptIn(false);
            userDTO.setMarketingAgreedAt(null);
        }
        userService.register(userDTO);
        CookieUtils.deleteCookie(request, response, "OAUTH2_TMP");
        CookieUtils.deleteCookie(request, response, "PHONE_VERIFIED_TMP");

        // 원래는 "redirect:/" 였는데, 백엔드에서 템플릿/프론트를 삭제했다면 프론트로 보내야 안전
        return "redirect:" + frontBaseUrl + "/";
    }

    /**
     * ✅ 컴파일 에러(enc missing) 해결용 - 최소 추가
     */
    private String enc(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return "";
        }
    }

    private static Map<String, Object> flowClaimsOf(Claims c) {
        Object claimsObj = c.get("claims");
        if (claimsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) claimsObj;
            return m;
        }
        return Collections.emptyMap();
    }
}
