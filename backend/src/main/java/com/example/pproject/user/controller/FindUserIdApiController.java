package com.example.pproject.user.controller;

import com.example.pproject.Config.CookieUtils;
import com.example.pproject.Config.JwtTokenProvider;
import com.example.pproject.sms.PhoneVerificationService;
import com.example.pproject.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/find-id")
@RequiredArgsConstructor
public class FindUserIdApiController {

    private final JwtTokenProvider jwtTokenProvider;
    private final PhoneVerificationService phoneVerificationService;
    private final UserService userService;

    private static final String PURPOSE_FIND_USERID = "FIND_USERID";

    /**
     * 1) 이름+휴대폰이 DB와 매칭되는지 확인
     * 2) 매칭되면 해당 휴대폰으로 OTP 발송
     * 3) OTP 정보를 HttpOnly 쿠키(FIND_USERID_OTP_TMP)에 저장
     */
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

        String name = body == null ? null : body.get("name");
        String phone = body == null ? null : body.get("phone");

        try {
            String trimmedName = name == null ? "" : name.trim();
            String normalizedPhone = normalize(phone);

            // ✅ 이름+휴대폰 매칭 검증(소셜 회원 제외 정책 포함)
            //    - 여기서 검증에 실패하면 SMS를 보내지 않음
            userService.findUseridByNameAndPhone(trimmedName, normalizedPhone);

            String ip = getClientIp(request);
            String ua = request.getHeader("User-Agent");

            var result = phoneVerificationService.sendOtp(normalizedPhone, PURPOSE_FIND_USERID, ip, ua);

            long ttl = Math.min(300, secondsUntil(result.expiresAt()));

            Map<String, Object> claims = new HashMap<>();
            claims.put("verificationId", result.verificationId().toString());
            claims.put("phone", normalizedPhone);
            claims.put("name", trimmedName);
            claims.put("purpose", PURPOSE_FIND_USERID);

            String tmp = jwtTokenProvider.createFlowToken("FIND_USERID_OTP", claims, ttl);
            CookieUtils.addHttpOnlyCookie(request, response, "FIND_USERID_OTP_TMP", tmp, ttl, "Lax");

            return ResponseEntity.ok(Map.of("ok", true, "expiresIn", ttl));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("ok", false, "message", "SMS 발송에 실패했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    /**
     * 1) 쿠키(FIND_USERID_OTP_TMP)에서 verificationId/phone/name 읽기
     * 2) OTP 검증
     * 3) 성공 시 userId(userid) 반환 + (선택) VERIFIED 쿠키 저장
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    @CookieValue(value = "FIND_USERID_OTP_TMP", required = false) String otpToken) {

        try {
            String phone = body == null ? null : body.get("phone");
            String code = body == null ? null : body.get("code");

            if (otpToken == null || otpToken.isBlank() || !jwtTokenProvider.validateToken(otpToken)) {
                return ResponseEntity.ok(Map.of("ok", false, "message", "인증번호를 먼저 발송해주세요."));
            }

            Claims c = jwtTokenProvider.getClaims(otpToken);
            if (!"FIND_USERID_OTP".equals(c.get("flowType", String.class))) {
                return ResponseEntity.ok(Map.of("ok", false, "message", "인증번호를 다시 발송해주세요."));
            }

            Map<String, Object> flow = flowClaimsOf(c);

            String vid = flow.get("verificationId") == null ? null : flow.get("verificationId").toString();
            String savedPhone = flow.get("phone") == null ? null : flow.get("phone").toString();
            String savedName = flow.get("name") == null ? null : flow.get("name").toString();
            String purpose = flow.get("purpose") == null ? null : flow.get("purpose").toString();

            String normalizedPhone = normalize(phone);

            if (vid == null || savedPhone == null || savedName == null || purpose == null) {
                return ResponseEntity.ok(Map.of("ok", false, "message", "인증번호를 다시 발송해주세요."));
            }
            if (!normalizedPhone.equals(savedPhone)) {
                return ResponseEntity.ok(Map.of("ok", false, "message", "휴대폰 번호가 일치하지 않습니다."));
            }
            if (!PURPOSE_FIND_USERID.equals(purpose)) {
                return ResponseEntity.ok(Map.of("ok", false, "message", "인증번호를 다시 발송해주세요."));
            }

            // ✅ OTP 검증
            phoneVerificationService.verifyOtp(UUID.fromString(vid), normalizedPhone, purpose, code);

            // ✅ OTP 검증 후, 이름+휴대폰으로 userid를 다시 조회(최종 확인)
            String userId = userService.findUseridByNameAndPhone(savedName, normalizedPhone);

            // (선택) 결과 페이지 새로고침을 대비해 VERIFIED 쿠키 저장(10분)
            Map<String, Object> verifiedClaims = new HashMap<>();
            verifiedClaims.put("phone", normalizedPhone);
            verifiedClaims.put("name", savedName);
            verifiedClaims.put("userid", userId);

            String verifiedToken = jwtTokenProvider.createFlowToken("FIND_USERID_VERIFIED", verifiedClaims, 600);
            CookieUtils.addHttpOnlyCookie(request, response, "FIND_USERID_VERIFIED_TMP", verifiedToken, 600, "Lax");

            CookieUtils.deleteCookie(request, response, "FIND_USERID_OTP_TMP");

            return ResponseEntity.ok(Map.of("ok", true, "userId", userId));

        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    /**
     * (선택) 결과 페이지에서 새로고침했을 때를 대비해, VERIFIED 쿠키에서 userid 조회
     */
    @GetMapping("/result")
    public ResponseEntity<?> result(@CookieValue(value = "FIND_USERID_VERIFIED_TMP", required = false) String verifiedToken) {
        try {
            if (verifiedToken == null || verifiedToken.isBlank() || !jwtTokenProvider.validateToken(verifiedToken)) {
                return ResponseEntity.ok(Map.of("ok", false, "message", "인증 정보가 없습니다."));
            }
            Claims c = jwtTokenProvider.getClaims(verifiedToken);
            if (!"FIND_USERID_VERIFIED".equals(c.get("flowType", String.class))) {
                return ResponseEntity.ok(Map.of("ok", false, "message", "인증 정보가 유효하지 않습니다."));
            }

            Map<String, Object> flow = flowClaimsOf(c);
            String userId = flow.get("userid") == null ? null : flow.get("userid").toString();
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.ok(Map.of("ok", false, "message", "아이디 정보가 없습니다."));
            }
            return ResponseEntity.ok(Map.of("ok", true, "userId", userId));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "message", "아이디 조회에 실패했습니다."));
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
}
