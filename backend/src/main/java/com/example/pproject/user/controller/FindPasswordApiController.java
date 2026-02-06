package com.example.pproject.user.controller;

import com.example.pproject.Config.JwtTokenProvider;
import com.example.pproject.sms.PhoneVerificationService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import com.example.pproject.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private final JavaMailSender mailSender;

    // DB CHECK 제약(SIGNUP/PASSWORD_RESET/PHONE_LINK)에 맞춤
    private static final String PURPOSE = "PASSWORD_RESET";

    private String normalizePhone(String v) {
        return String.valueOf(v == null ? "" : v).replaceAll("[^0-9]", "");
    }

    private long secondsUntil(OffsetDateTime exp) {
        if (exp == null) return 300;
        long sec = Duration.between(OffsetDateTime.now(), exp).getSeconds();
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

    private String enc(String v) {
        return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private boolean isLocalHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        return "localhost".equals(h) || "127.0.0.1".equals(h) || "::1".equals(h);
    }

    private boolean isInternalHostName(String host) {
        if (host == null || host.isBlank()) return true;
        String h = host.toLowerCase();
        if ("backend".equals(h)) return true;           // docker-compose service name
        if (isLocalHost(h)) return true;
        if (!h.contains(".") && !h.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) return true; // 점 없는 호스트명은 보통 내부 별칭
        return false;
    }

    private String firstHeader(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null || v.isBlank()) return null;
        return v.split(",")[0].trim();
    }

    private String baseFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI u = URI.create(url.trim());
            if (u.getScheme() == null || u.getHost() == null) return null;

            StringBuilder sb = new StringBuilder();
            sb.append(u.getScheme()).append("://").append(u.getHost());
            if (u.getPort() != -1) sb.append(":").append(u.getPort());
            return sb.toString().replaceAll("/+$", "");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 운영(프록시)에서 외부 도메인 자동 반영:
     * Forwarded/X-Forwarded-* 기반으로 외부 Base URL을 구성.
     */
    private String externalBaseUrl(HttpServletRequest request) {
        String proto = firstHeader(request, "X-Forwarded-Proto");
        String host  = firstHeader(request, "X-Forwarded-Host");
        String port  = firstHeader(request, "X-Forwarded-Port");

        String forwarded = request.getHeader("Forwarded");
        if ((proto == null || host == null) && forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            for (String part : first.split(";")) {
                String p = part.trim();
                int eq = p.indexOf('=');
                if (eq <= 0) continue;

                String k = p.substring(0, eq).trim().toLowerCase();
                String v = p.substring(eq + 1).trim().replace("\"", "");

                if ("proto".equals(k) && (proto == null || proto.isBlank())) proto = v;
                if ("host".equals(k)  && (host  == null || host.isBlank()))  host  = v;
            }
        }

        if (proto == null || proto.isBlank()) proto = request.getScheme();
        if (host  == null || host.isBlank())  host  = request.getServerName();

        // 최소한의 Host 이상치 방어
        if (!host.matches("^[A-Za-z0-9.\\-:\\[\\]]+$")) {
            host = request.getServerName();
        }

        String effectivePort = (port == null || port.isBlank())
                ? String.valueOf(request.getServerPort())
                : port;

        StringBuilder sb = new StringBuilder();
        sb.append(proto).append("://").append(host);

        boolean hostHasPort = host.contains(":") && !host.startsWith("[");
        if (!hostHasPort) {
            boolean isDefaultPort =
                    ("https".equalsIgnoreCase(proto) && "443".equals(effectivePort)) ||
                            ("http".equalsIgnoreCase(proto)  && "80".equals(effectivePort));
            if (!isDefaultPort) sb.append(":").append(effectivePort);
        }

        return sb.toString().replaceAll("/+$", "");
    }

    /**
     * 개발(Vite 프록시 changeOrigin=true)에서도 backend로 안 나가게:
     * - Origin이 내부호스트(backend)면 무시하고 Referer를 우선 사용
     * - Referer가 localhost:5173이면 메일 링크는 localhost:8080으로 보정
     */
    private String publicBackendBase(HttpServletRequest request) {
        String derived = externalBaseUrl(request);

        // 1) 운영 정상 케이스: derived의 host가 내부명이 아니면 그대로 사용
        String derivedHost = null;
        try {
            URI u = URI.create(derived);
            derivedHost = u.getHost();
        } catch (Exception ignore) {
        }
        if (derivedHost != null && !isInternalHostName(derivedHost)) {
            return derived;
        }

        // 2) 브라우저 기준 힌트: Referer 우선, Origin은 내부명이면 무시
        String refererBase = baseFromUrl(request.getHeader("Referer"));
        String originBase  = baseFromUrl(request.getHeader("Origin"));

        String chosen = null;

        if (originBase != null) {
            try {
                URI u = URI.create(originBase);
                String h = u.getHost();
                if (h != null && isInternalHostName(h)) {
                    // Origin이 backend로 바뀐 케이스 → Referer를 우선
                    chosen = (refererBase != null) ? refererBase : null;
                } else {
                    chosen = originBase;
                }
            } catch (Exception ignore) {
            }
        }
        if (chosen == null && refererBase != null) {
            chosen = refererBase;
        }

        // 3) 개발 보정: front(5173)에서 호출되면 메일 링크는 backend(8080)
        if (chosen != null) {
            if (chosen.startsWith("http://localhost:5173") || chosen.startsWith("http://127.0.0.1:5173")) {
                return "http://localhost:8080";
            }
            if (chosen.startsWith("https://localhost:5173") || chosen.startsWith("https://127.0.0.1:5173")) {
                return "https://localhost:8080";
            }

            // 운영에서 프론트 도메인으로 /User/... 라우팅하는 구조면 chosen 자체가 정답
            // (api 서브도메인 분리 구조는 완전 자동이 원천적으로 불가능하니 그 경우만 설정이 필요)
            return chosen;
        }

        // 4) 최종 fallback
        return "http://localhost:8080";
    }

    private String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf("@");
        if (at <= 1) return "****";
        String head = email.substring(0, Math.min(2, at));
        String domain = email.substring(at);
        return head + "****" + domain;
    }

    private void sendPasswordResetLinkEmail(String to, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("비밀번호 변경 링크");
        message.setText("아래 링크를 클릭하여 비밀번호를 변경하세요.\n\n" + link + "\n\n(유효시간: 15분)");
        mailSender.send(message);
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body,
                                  HttpServletRequest request) {
        try {
            String username = body == null ? null : body.get("username");
            String phone = normalizePhone(body == null ? null : body.get("phone"));

            if (username == null || username.isBlank()) {
                throw new IllegalStateException("이름을 입력해주세요.");
            }
            if (phone == null || phone.isBlank()) {
                throw new IllegalStateException("휴대폰 번호를 입력해주세요.");
            }

            UserEntity user = userRepository.findFirstByPhoneAndPhoneVerifiedAtIsNotNullAndDeletedAtIsNull(phone)
                    .orElseThrow(() -> new IllegalStateException("일치하는 회원 정보가 없습니다."));

            if (user.getUsername() == null || !user.getUsername().equals(username)) {
                throw new IllegalStateException("일치하는 회원 정보가 없습니다.");
            }
            if (user.getPhone() == null || user.getPhoneVerifiedAt() == null) {
                throw new IllegalStateException("휴대폰 인증된 계정이 아닙니다. 고객센터에 문의해주세요.");
            }
            if (!phone.equals(normalizePhone(user.getPhone()))) {
                throw new IllegalStateException("일치하는 회원 정보가 없습니다.");
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new IllegalStateException("소셜 로그인 계정입니다. 카카오/네이버/구글 로그인을 이용해주세요.");
            }
            if (user.getUserid() == null || user.getUserid().isBlank()) {
                throw new IllegalStateException("소셜 로그인 계정입니다. 카카오/네이버/구글 로그인을 이용해주세요.");
            }

            var sendResult = phoneVerificationService.sendOtp(
                    phone,
                    PURPOSE,
                    getClientIp(request),
                    request.getHeader("User-Agent")
            );

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "verificationId", sendResult.verificationId().toString(),
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
                                    HttpServletRequest request) {
        try {
            String username = body == null ? null : body.get("username");
            String phone = normalizePhone(body == null ? null : body.get("phone"));
            String code = body == null ? null : body.get("code");
            String verificationIdStr = body == null ? null : body.get("verificationId");

            if (username == null || username.isBlank()) {
                throw new IllegalStateException("이름을 입력해주세요.");
            }
            if (phone == null || phone.isBlank()) {
                throw new IllegalStateException("휴대폰 번호를 입력해주세요.");
            }
            if (code == null || code.isBlank()) {
                throw new IllegalStateException("인증번호를 입력해주세요.");
            }
            if (verificationIdStr == null || verificationIdStr.isBlank()) {
                throw new IllegalStateException("인증번호 발송부터 다시 진행해주세요.");
            }

            UserEntity user = userRepository.findFirstByPhoneAndPhoneVerifiedAtIsNotNullAndDeletedAtIsNull(phone)
                    .orElseThrow(() -> new IllegalStateException("일치하는 회원 정보가 없습니다."));

            if (user.getUsername() == null || !user.getUsername().equals(username)) {
                throw new IllegalStateException("일치하는 회원 정보가 없습니다.");
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new IllegalStateException("소셜 로그인 계정입니다. 카카오/네이버/구글 로그인을 이용해주세요.");
            }
            if (user.getUserid() == null || user.getUserid().isBlank()) {
                throw new IllegalStateException("소셜 로그인 계정입니다. 카카오/네이버/구글 로그인을 이용해주세요.");
            }

            String email = user.getEmail();
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("이메일 정보가 없습니다. 고객센터에 문의해주세요.");
            }

            UUID verificationId = UUID.fromString(verificationIdStr);
            phoneVerificationService.verifyOtp(verificationId, phone, PURPOSE, code);

            String linkToken = jwtTokenProvider.createFlowToken(
                    "PW_RESET_LINK",
                    Map.of("userid", user.getUserid(), "email", email),
                    900
            );

            String link = publicBackendBase(request) + "/User/Password_Reset_Link?token=" + enc(linkToken);
            sendPasswordResetLinkEmail(email, link);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "emailMasked", maskEmail(email)
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "message", "인증 확인에 실패했습니다."));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> body) {
        try {
            String token = body == null ? null : body.get("token");
            String newPassword = body == null ? null : body.get("newPassword");

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
