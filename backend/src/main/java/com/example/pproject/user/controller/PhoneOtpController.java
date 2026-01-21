package com.example.pproject.user.controller;

import com.example.pproject.Config.CookieUtils;
import com.example.pproject.Config.JwtTokenProvider;
import com.example.pproject.sms.PhoneVerificationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/phone/otp")
@RequiredArgsConstructor
public class PhoneOtpController {

    private final JwtTokenProvider jwtTokenProvider;
    private final PhoneVerificationService phoneVerificationService;

    private static final String PURPOSE_SIGNUP = "SIGNUP";
    private static final String PURPOSE_PROFILE_UPDATE = "PROFILE_UPDATE";

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

        String phone = body == null ? null : body.get("phone");
        // purpose가 없으면 기본값 SIGNUP 사용
        String purpose = body == null ? PURPOSE_SIGNUP : body.getOrDefault("purpose", PURPOSE_SIGNUP);
        // 허용된 목적만 사용
        if (!PURPOSE_SIGNUP.equals(purpose) && !PURPOSE_PROFILE_UPDATE.equals(purpose)) {
            purpose = PURPOSE_SIGNUP;
        }

        try {
            String ip = getClientIp(request);
            String ua = request.getHeader("User-Agent");

            var result = phoneVerificationService.sendOtp(phone, purpose, ip, ua);

            // 쿠키 TTL은 OTP 만료와 맞춤(최대 300s)
            long ttl = Math.min(300, secondsUntil(result.expiresAt()));

            Map<String, Object> claims = new HashMap<>();
            claims.put("verificationId", result.verificationId().toString());
            claims.put("phone", normalize(phone));
            claims.put("purpose", purpose);

            String tmp = jwtTokenProvider.createFlowToken("PHONE_OTP_DB", claims, ttl);
            CookieUtils.addHttpOnlyCookie(request, response, "PHONE_OTP_TMP", tmp, ttl, "Lax");

            return ResponseEntity.ok(Map.of("ok", true));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("ok", false, "message", "SMS 발송에 실패했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    @CookieValue(value = "PHONE_OTP_TMP", required = false) String otpToken) {

        try {
            String phone = body == null ? null : body.get("phone");
            String code = body == null ? null : body.get("code");

            if (otpToken == null || otpToken.isBlank() || !jwtTokenProvider.validateToken(otpToken)) {
                return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호를 먼저 발송해주세요."));
            }

            Claims c = jwtTokenProvider.getClaims(otpToken);
            if (!"PHONE_OTP_DB".equals(c.get("flowType", String.class))) {
                return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호를 다시 발송해주세요."));
            }

            Map<String, Object> flow = flowClaimsOf(c);
            String vid = flow.get("verificationId") == null ? null : flow.get("verificationId").toString();
            String savedPhone = flow.get("phone") == null ? null : flow.get("phone").toString();
            String purpose = flow.get("purpose") == null ? null : flow.get("purpose").toString();

            String normalizedPhone = normalize(phone);

            if (vid == null || savedPhone == null || purpose == null) {
                return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호를 다시 발송해주세요."));
            }
            if (!normalizedPhone.equals(savedPhone)) {
                return ResponseEntity.ok(Map.of("verified", false, "message", "휴대폰 번호가 일치하지 않습니다."));
            }

            phoneVerificationService.verifyOtp(UUID.fromString(vid), normalizedPhone, purpose, code);

            // ✅ 인증 성공: PHONE_VERIFIED_TMP (15분)
            Map<String, Object> verifiedClaims = new HashMap<>();
            verifiedClaims.put("phone", normalizedPhone);
            verifiedClaims.put("purpose", purpose);

            String verified = jwtTokenProvider.createFlowToken("PHONE_VERIFIED", verifiedClaims, 900);
            CookieUtils.addHttpOnlyCookie(request, response, "PHONE_VERIFIED_TMP", verified, 900, "Lax");

            CookieUtils.deleteCookie(request, response, "PHONE_OTP_TMP");
            return ResponseEntity.ok(Map.of("verified", true));

        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("verified", false, "message", e.getMessage()));
        }
    }

    public static String readVerifiedPhone(JwtTokenProvider jwtTokenProvider, String verifiedToken, String requiredPurpose) {
        if (verifiedToken == null || verifiedToken.isBlank() || jwtTokenProvider == null) return null;
        if (!jwtTokenProvider.validateToken(verifiedToken)) return null;

        Claims c = jwtTokenProvider.getClaims(verifiedToken);
        if (!"PHONE_VERIFIED".equals(c.get("flowType", String.class))) return null;

        Map<String, Object> flow = flowClaimsOf(c);
        String phone = flow.get("phone") == null ? null : flow.get("phone").toString();
        String purpose = flow.get("purpose") == null ? null : flow.get("purpose").toString();

        if (phone == null) return null;
        if (requiredPurpose != null && !requiredPurpose.equals(purpose)) return null;

        return phone;
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

    private String normalize(String p) {
        return String.valueOf(p == null ? "" : p).replaceAll("[^0-9]", "");
    }

    private long secondsUntil(OffsetDateTime exp) {
        if (exp == null) return 300;
        long sec = java.time.Duration.between(OffsetDateTime.now(), exp).getSeconds();
        return Math.max(1, sec);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(
            @CookieValue(value = "PHONE_VERIFIED_TMP", required = false) String verifiedToken
    ) {
        String phone = readVerifiedPhone(jwtTokenProvider, verifiedToken, "SIGNUP");
        boolean verified = (phone != null);
        return ResponseEntity.ok(Map.of("verified", verified));
    }
}