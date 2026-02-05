package com.example.pproject.auth.controller;

import com.example.pproject.user.service.UserService;
import com.example.pproject.Config.CookieUtils;
import com.example.pproject.Config.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;

import java.util.Random;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserService userService;
    private final JavaMailSender mailSender;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    /**
     * 외부(배포)에서 접근 가능한 백엔드 공개 Base URL.
     * - 개발: 기본값 http://localhost:8080
     * - 운영: https://api.example.com 또는 https://example.com 등으로 설정
     */
    @Value("${app.public-backend-url:http://localhost:8080}")
    private String publicBackendUrl;

    private String redirectFront(String path) {
        return "redirect:" + frontBaseUrl + path;
    }

    private String redirectFrontWithQuery(String path, String query) {
        if (query == null || query.isBlank()) return redirectFront(path);
        return "redirect:" + frontBaseUrl + path + "?" + query;
    }

    private String enc(String v) {
        return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
private boolean isLocalHost(String host) {
    if (host == null) return false;
    String h = host.toLowerCase();
    return "localhost".equals(h) || "127.0.0.1".equals(h) || "::1".equals(h);
}

private String firstHeader(HttpServletRequest request, String name) {
    String v = request.getHeader(name);
    if (v == null || v.isBlank()) return null;
    // 일부 프록시는 "a,b" 형태로 전달
    return v.split(",")[0].trim();
}

/**
 * 외부(사용자 브라우저/이메일에서) 접근 가능한 Base URL을 자동 추론한다.
 * - 우선순위: Forwarded / X-Forwarded-* 헤더 → request scheme/host/port
 * - (개발) Vite 프록시 Host=5173 같은 케이스는 app.public-backend-url(기본 localhost:8080)로 보정
 */
private String externalBaseUrl(HttpServletRequest request) {
    String proto = firstHeader(request, "X-Forwarded-Proto");
    String host = firstHeader(request, "X-Forwarded-Host");
    String port = firstHeader(request, "X-Forwarded-Port");

    String forwarded = request.getHeader("Forwarded");
    if ((proto == null || host == null) && forwarded != null && !forwarded.isBlank()) {
        // Forwarded: proto=https;host=example.com:443
        String first = forwarded.split(",")[0].trim();
        for (String part : first.split(";")) {
            String p = part.trim();
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String k = p.substring(0, eq).trim().toLowerCase();
            String v = p.substring(eq + 1).trim().replace(""", "");
            if ("proto".equals(k) && (proto == null || proto.isBlank())) proto = v;
            if ("host".equals(k) && (host == null || host.isBlank())) host = v;
        }
    }

    if (proto == null || proto.isBlank()) proto = request.getScheme();
    if (host == null || host.isBlank()) host = request.getServerName();

    // host에 이미 포트가 포함되어 있을 수 있음
    String effectivePort = port;
    if (effectivePort == null || effectivePort.isBlank()) effectivePort = String.valueOf(request.getServerPort());

    StringBuilder sb = new StringBuilder();
    sb.append(proto).append("://").append(host);
    if (!host.contains(":")) {
        boolean isDefaultPort =
                ("https".equalsIgnoreCase(proto) && "443".equals(effectivePort))
                        || ("http".equalsIgnoreCase(proto) && "80".equals(effectivePort));
        if (!isDefaultPort) sb.append(":").append(effectivePort);
    }
    return sb.toString().replaceAll("/+$", "");
}

private String publicBackendBase(HttpServletRequest request) {
    // 운영에서 env/app.yml 미설정이어도 "localhost 기본값"이 링크에 박히지 않도록:
    // - 요청이 외부 도메인이면, 기본 localhost 설정은 무시하고 헤더 기반으로 추론
    String derived = externalBaseUrl(request);

    String cfg = (publicBackendUrl == null ? "" : publicBackendUrl).trim();
    if (!cfg.isBlank()) {
        cfg = cfg.replaceAll("/+$", "");
        boolean cfgIsLocal = cfg.contains("localhost") || cfg.contains("127.0.0.1");
        boolean reqIsLocal = isLocalHost(request.getServerName());

        if (!cfgIsLocal) return cfg;        // 명시 설정(운영)
        if (reqIsLocal) return cfg;         // 개발(localhost)
        // cfg는 localhost인데 요청은 외부 도메인 → cfg 무시
    }
    return derived;
}

private String effectiveFrontBase(HttpServletRequest request) {
    // 기본값(localhost:5173)이 운영에서 남아있어도 자동 추론되도록 처리
    String cfg = (frontBaseUrl == null ? "" : frontBaseUrl).trim();
    if (!cfg.isBlank()) {
        cfg = cfg.replaceAll("/+$", "");
        boolean cfgIsLocal = cfg.contains("localhost") || cfg.contains("127.0.0.1");
        boolean reqIsLocal = isLocalHost(request.getServerName());
        if (!cfgIsLocal) return cfg;
        if (reqIsLocal) return cfg;
    }

    String derived = externalBaseUrl(request);
    try {
        URI u = URI.create(derived);
        String host = u.getHost();
        String scheme = u.getScheme();
        int port = u.getPort();
        if (host != null && host.startsWith("api.")) {
            host = host.substring(4);
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host);
            if (port != -1) {
                boolean isDefaultPort =
                        ("https".equalsIgnoreCase(scheme) && port == 443)
                                || ("http".equalsIgnoreCase(scheme) && port == 80);
                if (!isDefaultPort) sb.append(":").append(port);
            }
            return sb.toString();
        }
    } catch (Exception ignored) { }
    return derived;
}
        return baseUrl(request);
    }

    private java.util.Map<String, Object> flowClaimsOf(Claims c) {
        Object claimsObj = c.get("claims");
        if (claimsObj instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> m = (java.util.Map<String, Object>) claimsObj;
            return m;
        }
        return java.util.Collections.emptyMap();
    }

    @GetMapping("/User/Find_Password")
    public String findPasswordForm() {
        return redirectFront("/FindPassword");
    }

    /**
     * ✅ 비밀번호 찾기(링크 발송)
     * - (아이디, 이름, 이메일) 검증 후 이메일로 "비밀번호 변경 페이지 링크"를 전송한다.
     * - 링크는 백엔드 엔드포인트(/User/Password_Reset_Link)를 거쳐 HttpOnly 쿠키를 세팅한 뒤 프론트(/NewPassword)로 리다이렉트한다.
     */
    @PostMapping("/User/Find_Password_Link")
    public ResponseEntity<?> findPasswordLink(@RequestParam String userid,
                                              @RequestParam String username,
                                              @RequestParam String email,
                                              HttpServletRequest request) {
        try {
            String verifiedEmail = userService.findEmailByUseridAndUsernameAndEmailForPasswordLink(userid, username, email);

            java.util.Map<String, Object> claims = new java.util.HashMap<>();
            claims.put("userid", userid);
            claims.put("email", verifiedEmail);

            // 15분 유효
            String token = jwtTokenProvider.createFlowToken("PW_RESET_LINK", claims, 900);
            String link = publicBackendBase(request) + "/User/Password_Reset_Link?token=" + enc(token);

            sendPasswordResetLinkEmail(verifiedEmail, link);
            return ResponseEntity.ok(java.util.Map.of("ok", true));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "ok", false,
                    "message", e.getMessage() == null ? "요청 처리에 실패했습니다." : e.getMessage()
            ));
        }
    }

    /**
     * ✅ 이메일 링크 클릭 엔드포인트
     * - token 검증 후, PW_RESET_TMP(HttpOnly) 쿠키를 세팅하고 프론트(/NewPassword)로 리다이렉트
     */
    @GetMapping("/User/Password_Reset_Link")
    public String openPasswordResetLink(@RequestParam String token,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {

        if (token == null || token.isBlank() || !jwtTokenProvider.validateToken(token)) {
            return "redirect:" + effectiveFrontBase(request) + "/FindPassword?error=" + enc("링크가 만료되었거나 올바르지 않습니다.");
        }

        Claims c = jwtTokenProvider.getClaims(token);
        if (!"PW_RESET_LINK".equals(c.get("flowType", String.class))) {
            return "redirect:" + effectiveFrontBase(request) + "/FindPassword?error=" + enc("링크가 만료되었거나 올바르지 않습니다.");
        }

        java.util.Map<String, Object> flow = flowClaimsOf(c);
        String userid = flow.get("userid") == null ? null : flow.get("userid").toString();
        String email = flow.get("email") == null ? null : flow.get("email").toString();

        if (userid == null || userid.isBlank() || email == null || email.isBlank()) {
            return "redirect:" + effectiveFrontBase(request) + "/FindPassword?error=" + enc("링크가 만료되었거나 올바르지 않습니다.");
        }

        // ✅ NewPassword 제출 시 검증할 쿠키(PW_RESET_TMP) 발급
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userid", userid);
        claims.put("email", email);

        String tmp = jwtTokenProvider.createFlowToken("PW_RESET", claims, 900);
        CookieUtils.addHttpOnlyCookie(request, response, "PW_RESET_TMP", tmp, 900, "Lax");

        return "redirect:" + effectiveFrontBase(request) + "/NewPassword?userid=" + enc(userid);
}

    @PostMapping("/User/Find_Password")
    public String findPassword(@RequestParam String userid,
                               @RequestParam String username,
                               @RequestParam String birthday,
                               Model model,
                               HttpServletRequest request,
                               HttpServletResponse response) {

        // 아이디/성함/생일로 사용자 확인 후 이메일 조회
        String email = userService.findEmailByUseridAndUsernameAndBirthday(userid, username, birthday);

        if (email != null) {
            String code = generateRandomCode();

            // ✅ 단기 플로우 토큰(JWT)에 저장 → HttpOnly 쿠키로 전달 (무세션)
            java.util.Map<String, Object> claims = new java.util.HashMap<>();
            claims.put("email", email);
            claims.put("code", code);
            claims.put("userid", userid);
            String tmp = jwtTokenProvider.createFlowToken("PW_RESET", claims, 600);
            CookieUtils.addHttpOnlyCookie(request, response, "PW_RESET_TMP", tmp, 600, "Lax");

            sendVerificationEmail(email, code);

            return redirectFrontWithQuery("/VerifyCode", "userid=" + enc(userid));
        }

        return redirectFrontWithQuery("/FindPassword", "error=" + enc("정보가 일치하지 않습니다."));
    }

    @PostMapping("/User/Verify_Code")
    public String verifyCode(@RequestParam String inputCode,
                             Model model,
                             @CookieValue(value = "PW_RESET_TMP", required = false) String tmpToken) {

        if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        Claims c = jwtTokenProvider.getClaims(tmpToken);
        if (!"PW_RESET".equals(c.get("flowType", String.class))) {
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        java.util.Map<String, Object> flowClaims = flowClaimsOf(c);

        String savedCode = flowClaims.get("code") == null ? null : flowClaims.get("code").toString();
        String userid = flowClaims.get("userid") == null ? null : flowClaims.get("userid").toString();

        if (savedCode == null || userid == null) {
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        if (inputCode.equals(savedCode)) {
            return "redirect:" + effectiveFrontBase(request) + "/NewPassword?userid=" + enc(userid);
}

        return redirectFrontWithQuery("/VerifyCode", "userid=" + enc(userid) + "&error=" + enc("인증번호가 일치하지 않습니다."));
    }

    @PostMapping("/User/New_Password")
    public String resetPassword(@RequestParam String userid,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                @CookieValue(value = "PW_RESET_TMP", required = false) String tmpToken) {

        // ✅ 링크/인증코드 플로우 공통: 임시 쿠키가 있어야만 비밀번호 변경 허용
        if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        Claims c = jwtTokenProvider.getClaims(tmpToken);
        if (!"PW_RESET".equals(c.get("flowType", String.class))) {
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        java.util.Map<String, Object> flow = flowClaimsOf(c);
        String cookieUserid = flow.get("userid") == null ? null : flow.get("userid").toString();
        if (cookieUserid == null || cookieUserid.isBlank() || !cookieUserid.equals(userid)) {
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        // 입력값 일치 여부 확인
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("userid", userid);
            model.addAttribute("error", "비밀번호가 서로 다릅니다.");
            return redirectFrontWithQuery("/NewPassword", "userid=" + enc(userid) + "&error=" + enc("비밀번호가 서로 다릅니다."));
        }

        // 실제 업데이트
        userService.updatePassword(userid, newPassword);

        // ✅ 플로우 쿠키 정리
        CookieUtils.deleteCookie(request, response, "PW_RESET_TMP");

        // 프록시(changeOrigin) 환경에서 상대 redirect("/Login")가 backend:8080로 튀는 문제를 방지
        return redirectFrontWithQuery("/Login", null);
    }

    private String generateRandomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private void sendVerificationEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("비밀번호 찾기 인증번호");
        message.setText("인증번호: " + code);
        mailSender.send(message);
    }

    private void sendPasswordResetLinkEmail(String to, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("비밀번호 변경 링크");
        message.setText("아래 링크를 클릭하여 비밀번호를 변경하세요.\n\n" + link + "\n\n(유효시간: 15분)");
        mailSender.send(message);
    }

    // ============================================
    // REST API 엔드포인트 (프론트엔드용)
    // ============================================

    /**
     * [REST API] 비밀번호 재설정 요청 - 인증코드 발송
     * POST /api/auth/password/reset/request
     */
    @PostMapping("/api/auth/password/reset/request")
    public ResponseEntity<?> apiRequestPasswordReset(
            @RequestParam String userid,
            @RequestParam String username,
            @RequestParam String birthday,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String email = userService.findEmailByUseridAndUsernameAndBirthday(userid, username, birthday);
            
            if (email == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "일치하는 사용자 정보를 찾을 수 없습니다."
                ));
            }

            String code = generateRandomCode();

            // 플로우 토큰 생성
            java.util.Map<String, Object> claims = new java.util.HashMap<>();
            claims.put("email", email);
            claims.put("code", code);
            claims.put("userid", userid);
            String tmp = jwtTokenProvider.createFlowToken("PW_RESET", claims, 600);
            CookieUtils.addHttpOnlyCookie(request, response, "PW_RESET_TMP", tmp, 600, "Lax");

            sendVerificationEmail(email, code);

            return ResponseEntity.ok(java.util.Map.of(
                    "ok", true,
                    "message", "인증코드가 이메일로 발송되었습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "ok", false,
                    "message", e.getMessage() == null ? "요청 처리에 실패했습니다." : e.getMessage()
            ));
        }
    }

    /**
     * [REST API] 인증코드 확인
     * POST /api/auth/password/reset/verify
     */
    @PostMapping("/api/auth/password/reset/verify")
    public ResponseEntity<?> apiVerifyCode(
            @RequestParam String userid,
            @RequestParam String inputCode,
            @CookieValue(value = "PW_RESET_TMP", required = false) String tmpToken) {
        try {
            if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "인증 절차가 만료되었습니다. 다시 시도해주세요."
                ));
            }

            Claims c = jwtTokenProvider.getClaims(tmpToken);
            if (!"PW_RESET".equals(c.get("flowType", String.class))) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "인증 절차가 만료되었습니다. 다시 시도해주세요."
                ));
            }

            java.util.Map<String, Object> flowClaims = flowClaimsOf(c);
            String savedCode = flowClaims.get("code") == null ? null : flowClaims.get("code").toString();
            String savedUserid = flowClaims.get("userid") == null ? null : flowClaims.get("userid").toString();

            if (savedCode == null || savedUserid == null || !savedUserid.equals(userid)) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "인증 절차가 만료되었습니다. 다시 시도해주세요."
                ));
            }

            if (!inputCode.equals(savedCode)) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "인증번호가 일치하지 않습니다."
                ));
            }

            return ResponseEntity.ok(java.util.Map.of(
                    "ok", true,
                    "message", "인증이 완료되었습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "ok", false,
                    "message", e.getMessage() == null ? "요청 처리에 실패했습니다." : e.getMessage()
            ));
        }
    }

    /**
     * [REST API] 비밀번호 변경
     * POST /api/auth/password/reset/confirm
     */
    @PostMapping("/api/auth/password/reset/confirm")
    public ResponseEntity<?> apiConfirmPasswordReset(
            @RequestParam String userid,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpServletRequest request,
            HttpServletResponse response,
            @CookieValue(value = "PW_RESET_TMP", required = false) String tmpToken) {
        try {
            if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "인증 절차가 만료되었습니다. 다시 시도해주세요."
                ));
            }

            Claims c = jwtTokenProvider.getClaims(tmpToken);
            if (!"PW_RESET".equals(c.get("flowType", String.class))) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "인증 절차가 만료되었습니다. 다시 시도해주세요."
                ));
            }

            java.util.Map<String, Object> flow = flowClaimsOf(c);
            String cookieUserid = flow.get("userid") == null ? null : flow.get("userid").toString();
            if (cookieUserid == null || cookieUserid.isBlank() || !cookieUserid.equals(userid)) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "인증 절차가 만료되었습니다. 다시 시도해주세요."
                ));
            }

            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "ok", false,
                        "message", "비밀번호가 서로 일치하지 않습니다."
                ));
            }

            userService.updatePassword(userid, newPassword);
            CookieUtils.deleteCookie(request, response, "PW_RESET_TMP");

            return ResponseEntity.ok(java.util.Map.of(
                    "ok", true,
                    "message", "비밀번호가 성공적으로 변경되었습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "ok", false,
                    "message", e.getMessage() == null ? "요청 처리에 실패했습니다." : e.getMessage()
            ));
        }
    }
}