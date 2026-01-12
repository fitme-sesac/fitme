package com.example.pproject.user.controller;

import com.example.pproject.Config.CookieUtils;
import com.example.pproject.Config.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 휴대폰 인증(OTP) API
 * - 외부 SMS 연동 전까지는 devCode를 응답에 포함(프론트에서 개발용 표시)
 * - 검증 성공 시 PHONE_VERIFIED_TMP(HttpOnly) 쿠키를 발급
 */
@RestController
@RequestMapping("/api/phone/otp")
@RequiredArgsConstructor
public class PhoneOtpController {

    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        String phone = body == null ? null : body.get("phone");
        String normalizedPhone = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (normalizedPhone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "휴대폰 번호를 입력해주세요."));
        }

        String code = generate6Digits();
        Map<String, Object> claims = new HashMap<>();
        claims.put("phone", normalizedPhone);
        claims.put("code", code);

        // 5분 유효
        String tmp = jwtTokenProvider.createFlowToken("PHONE_OTP", claims, 300);
        CookieUtils.addHttpOnlyCookie(request, response, "PHONE_OTP_TMP", tmp, 300, "Lax");

        // TODO: 실제 SMS 발송 연동(현재는 devCode 반환)
        return ResponseEntity.ok(Map.of("ok", true, "devCode", code));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    @CookieValue(value = "PHONE_OTP_TMP", required = false) String otpToken) {

        String phone = body == null ? null : body.get("phone");
        String code = body == null ? null : body.get("code");
        String normalizedPhone = phone == null ? "" : phone.replaceAll("[^0-9]", "");

        if (normalizedPhone.isBlank() || code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("verified", false, "message", "휴대폰 번호와 인증번호를 입력해주세요."));
        }
        if (otpToken == null || otpToken.isBlank() || !jwtTokenProvider.validateToken(otpToken)) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호를 먼저 발송해주세요."));
        }

        Claims c = jwtTokenProvider.getClaims(otpToken);
        if (!"PHONE_OTP".equals(c.get("flowType", String.class))) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호를 다시 발송해주세요."));
        }

        Map<String, Object> flow = flowClaimsOf(c);
        String savedPhone = flow.get("phone") == null ? null : flow.get("phone").toString();
        String savedCode = flow.get("code") == null ? null : flow.get("code").toString();

        if (savedPhone == null || savedCode == null) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호를 다시 발송해주세요."));
        }
        if (!normalizedPhone.equals(savedPhone)) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "휴대폰 번호가 일치하지 않습니다."));
        }
        if (!code.equals(savedCode)) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 일치하지 않습니다."));
        }

        // ✅ 인증 성공: verified 쿠키 발급(15분)
        Map<String, Object> verifiedClaims = new HashMap<>();
        verifiedClaims.put("phone", normalizedPhone);
        String verified = jwtTokenProvider.createFlowToken("PHONE_VERIFIED", verifiedClaims, 900);
        CookieUtils.addHttpOnlyCookie(request, response, "PHONE_VERIFIED_TMP", verified, 900, "Lax");

        CookieUtils.deleteCookie(request, response, "PHONE_OTP_TMP");
        return ResponseEntity.ok(Map.of("verified", true));
    }

    /**
     * 컨트롤러/서비스에서 재사용하기 위한 "PHONE_VERIFIED_TMP" 해석 함수
     */
    public static String readVerifiedPhone(JwtTokenProvider jwtTokenProvider, String verifiedToken) {
        if (verifiedToken == null || verifiedToken.isBlank() || jwtTokenProvider == null) return null;
        if (!jwtTokenProvider.validateToken(verifiedToken)) return null;

        Claims c = jwtTokenProvider.getClaims(verifiedToken);
        if (!"PHONE_VERIFIED".equals(c.get("flowType", String.class))) return null;

        Map<String, Object> flow = flowClaimsOf(c);
        Object p = flow.get("phone");
        return p == null ? null : p.toString();
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

    private static String generate6Digits() {
        int n = 100000 + new Random().nextInt(900000);
        return String.valueOf(n);
    }
}
