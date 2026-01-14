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

    /**
     * Initiates a signup phone OTP: sends an OTP to the provided phone number, stores a short-lived flow token in an HttpOnly cookie, and returns an acknowledgement.
     *
     * @param body a JSON-like map expected to contain the key "phone" with the recipient phone number as its value
     * @param request the incoming HTTP request (used to derive client IP and headers)
     * @param response the HTTP response used to set the HttpOnly cookie "PHONE_OTP_TMP"
     * @return a ResponseEntity whose body is a map; on success the map is {@code {"ok": true}}; on validation errors responds with HTTP 400 and {@code {"ok": false, "message": ...}}; on other failures responds with HTTP 502 and {@code {"ok": false, "message": "SMS 발송에 실패했습니다. 잠시 후 다시 시도해주세요."}}
     */
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

        String phone = body == null ? null : body.get("phone");

        try {
            String ip = getClientIp(request);
            String ua = request.getHeader("User-Agent");

            var result = phoneVerificationService.sendOtp(phone, PURPOSE_SIGNUP, ip, ua);

            // 쿠키 TTL은 OTP 만료와 맞춤(최대 300s)
            long ttl = Math.min(300, secondsUntil(result.expiresAt()));

            Map<String, Object> claims = new HashMap<>();
            claims.put("verificationId", result.verificationId().toString());
            claims.put("phone", normalize(phone));
            claims.put("purpose", PURPOSE_SIGNUP);

            String tmp = jwtTokenProvider.createFlowToken("PHONE_OTP_DB", claims, ttl);
            CookieUtils.addHttpOnlyCookie(request, response, "PHONE_OTP_TMP", tmp, ttl, "Lax");

            return ResponseEntity.ok(Map.of("ok", true));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("ok", false, "message", "SMS 발송에 실패했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    /**
     * Verify a previously sent phone OTP and, on success, issue a short-lived verified-phone cookie.
     *
     * <p>Expects a JSON body with "phone" and "code". Validates the "PHONE_OTP_TMP" flow token from a cookie,
     * ensures the phone matches the token's saved phone, and delegates OTP verification to the service.
     * On successful verification sets a "PHONE_VERIFIED_TMP" HttpOnly cookie and removes "PHONE_OTP_TMP".</p>
     *
     * @param body     request JSON body containing "phone" and "code"
     * @param otpToken the value of the "PHONE_OTP_TMP" cookie; a flow token produced by the OTP send step
     * @return         a map with "verified": `true` when verification succeeded, `false` otherwise; when `false` includes
     *                 a "message" string describing the failure reason.
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

    /**
     * Extracts the verified phone number from a PHONE_VERIFIED flow token when the token is valid and its purpose matches the caller's requirement.
     *
     * @param verifiedToken     the JWT flow token (typically from the PHONE_VERIFIED_TMP cookie); may be null or blank
     * @param requiredPurpose   if non-null, the token's purpose must equal this value for the phone to be returned
     * @return                  the verified phone string if the token is valid, has flowType "PHONE_VERIFIED", contains a phone, and matches the required purpose (if provided); `null` otherwise
     */
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

    /**
     * Extracts the nested "claims" map from a JWT Claims object.
     *
     * @param c the JWT Claims to read the nested claims from
     * @return the nested claims as a Map if present and a Map, otherwise an empty map
     */
    private static Map<String, Object> flowClaimsOf(Claims c) {
        Object claimsObj = c.get("claims");
        if (claimsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) claimsObj;
            return m;
        }
        return Collections.emptyMap();
    }

    /**
     * Normalize a phone string by removing all non-digit characters.
     *
     * @param p the input phone string (may be null)
     * @return a string containing only digits from the input; empty if the input is null or contains no digits
     */
    private String normalize(String p) {
        return String.valueOf(p == null ? "" : p).replaceAll("[^0-9]", "");
    }

    /**
     * Compute seconds until the provided expiration timestamp.
     *
     * If `exp` is null, returns 300. If `exp` is in the past or less than one second away, returns 1.
     *
     * @param exp the expiration timestamp to compare against, or `null` to use the default TTL
     * @return the number of seconds until expiration (minimum 1); 300 when `exp` is null
     */
    private long secondsUntil(OffsetDateTime exp) {
        if (exp == null) return 300;
        long sec = java.time.Duration.between(OffsetDateTime.now(), exp).getSeconds();
        return Math.max(1, sec);
    }

    /**
     * Determines the client's IP address, preferring the first value in the `X-Forwarded-For` header when present.
     *
     * If the `X-Forwarded-For` header is missing or blank, returns the request's remote address.
     *
     * @param request the HTTP servlet request
     * @return the client's IP address (first `X-Forwarded-For` entry, trimmed, or `request.getRemoteAddr()` if unavailable)
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    /**
     * Checks whether a phone number has been verified for the signup purpose.
     *
     * @param verifiedToken the value of the `PHONE_VERIFIED_TMP` cookie containing the verification flow token; may be null
     * @return a map with a single entry `"verified"` set to `true` if a valid verified phone for signup exists, `false` otherwise
     */
    @GetMapping("/status")
    public ResponseEntity<?> status(
            @CookieValue(value = "PHONE_VERIFIED_TMP", required = false) String verifiedToken
    ) {
        String phone = readVerifiedPhone(jwtTokenProvider, verifiedToken, "SIGNUP");
        boolean verified = (phone != null);
        return ResponseEntity.ok(Map.of("verified", verified));
    }
}