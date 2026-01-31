package com.example.pproject.user.controller;

import com.example.pproject.Config.CookieUtils;
import com.example.pproject.Config.JwtTokenProvider;
import com.example.pproject.sms.PhoneVerificationService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import com.example.pproject.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/find-password")
public class FindPasswordApiController {

    private final PhoneVerificationService phoneVerificationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserService userService;

    private static final String PURPOSE = "FIND_PASSWORD";
    private static final String COOKIE_OTP_TMP = "FIND_PASSWORD_OTP_TMP";

    private String normalizePhone(String v) {
        return String.valueOf(v == null ? "" : v).replaceAll("[^0-9]", "");
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFlowClaims(Claims c) {
        Object obj = c.get("claims");
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return java.util.Collections.emptyMap();
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        try {
            String loginId = body.get("loginId");
            String phone = normalizePhone(body.get("phone"));

            if (loginId == null || loginId.isBlank()) {
                throw new IllegalStateException("아이디를 입력해주세요.");
            }
            if (phone.isBlank()) {
                throw new IllegalStateException("휴대폰 번호를 입력해주세요.");
            }

            UserEntity user = userRepository.findByUserid(loginId)
                    .orElseThrow(() -> new IllegalStateException("일치하는 회원 정보가 없습니다."));

            if (user.getDeletedAt() != null) {
                throw new IllegalStateException("일치하는 회원 정보가 없습니다.");
            }
            if (user.getPhone() == null || user.getPhone().isBlank() || user.getPhoneVerifiedAt() == null) {
                throw new IllegalStateException("휴대폰 인증된 계정이 아닙니다. 고객센터에 문의해주세요.");
            }
            if (!phone.equals(normalizePhone(user.getPhone()))) {
                throw new IllegalStateException("일치하는 회원 정보가 없습니다.");
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new IllegalStateException("소셜 로그인 계정입니다. 카카오/네이버/구글 로그인을 이용해주세요.");
            }

            var sendResult = phoneVerificationService.sendOtp(
                    phone,
                    PURPOSE,
                    getClientIp(request),
                    request.getHeader("User-Agent")
            );

            String tmpToken = jwtTokenProvider.createFlowToken(
                    PURPOSE,
                    Map.of(
                            "verificationId", sendResult.verificationId().toString(),
                            "phone", phone,
                            "userid", loginId
                    ),
                    600
            );

            CookieUtils.addHttpOnlyCookie(request, response, COOKIE_OTP_TMP, tmpToken, 600, "Lax");

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "expiresIn", secondsUntil(sendResult.expiresAt())
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "message", "인증번호 발송에 실패했습니다."));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    @CookieValue(value = COOKIE_OTP_TMP, required = false) String tmpToken) {
        try {
            if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
                throw new IllegalStateException("인증 절차가 만료되었습니다. 다시 발송해주세요.");
            }

            Claims c = jwtTokenProvider.getClaims(tmpToken);
            if (!PURPOSE.equals(c.get("flowType", String.class))) {
                throw new IllegalStateException("인증 절차가 만료되었습니다. 다시 발송해주세요.");
            }

            Map<String, Object> claims = getFlowClaims(c);

            String phone = normalizePhone(body.get("phone"));
            String code = body.get("code");

            String claimPhone = claims.get("phone") == null ? null : String.valueOf(claims.get("phone"));
            String verificationIdStr = claims.get("verificationId") == null ? null : String.valueOf(claims.get("verificationId"));
            String userid = claims.get("userid") == null ? null : String.valueOf(claims.get("userid"));

            if (claimPhone == null || verificationIdStr == null || userid == null) {
                throw new IllegalStateException("인증 절차가 만료되었습니다. 다시 발송해주세요.");
            }
            if (!claimPhone.equals(phone)) {
                throw new IllegalStateException("인증 정보가 일치하지 않습니다. 다시 발송해주세요.");
            }

            UUID verificationId = UUID.fromString(verificationIdStr);
            phoneVerificationService.verifyOtp(verificationId, phone, PURPOSE, code);

            // OTP tmp 쿠키는 폐기
            CookieUtils.deleteCookie(request, response, COOKIE_OTP_TMP);

            // 10분 유효 재설정 토큰 발급
            String resetToken = jwtTokenProvider.createFlowToken(
                    "FIND_PASSWORD_RESET",
                    Map.of("userid", userid),
                    600
            );

            return ResponseEntity.ok(Map.of("ok", true, "token", resetToken));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "message", "인증 확인에 실패했습니다."));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newPassword = body.get("newPassword");

            if (token == null || token.isBlank() || !jwtTokenProvider.validateToken(token)) {
                throw new IllegalStateException("재설정 토큰이 만료되었습니다. 다시 시도해주세요.");
            }

            Claims c = jwtTokenProvider.getClaims(token);
            if (!"FIND_PASSWORD_RESET".equals(c.get("flowType", String.class))) {
                throw new IllegalStateException("재설정 토큰이 유효하지 않습니다. 다시 시도해주세요.");
            }

            Map<String, Object> claims = getFlowClaims(c);
            String userid = claims.get("userid") == null ? null : String.valueOf(claims.get("userid"));
            if (userid == null || userid.isBlank()) {
                throw new IllegalStateException("재설정 토큰이 유효하지 않습니다. 다시 시도해주세요.");
            }

            if (newPassword == null || newPassword.length() < 8) {
                throw new IllegalStateException("비밀번호는 8자 이상이어야 합니다.");
            }

            userService.updatePassword(userid, newPassword);

            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "message", "비밀번호 변경에 실패했습니다."));
        }
    }
}
