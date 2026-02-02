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

    private String baseUrl(HttpServletRequest request) {
        // e.g. http://localhost:8080
        String url = request.getRequestURL().toString();
        String uri = request.getRequestURI();
        if (url.endsWith(uri)) {
            return url.substring(0, url.length() - uri.length());
        }
        int idx = url.indexOf("/User/");
        return idx > 0 ? url.substring(0, idx) : url;
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
            String link = baseUrl(request) + "/User/Password_Reset_Link?token=" + enc(token);

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
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("링크가 만료되었거나 올바르지 않습니다."));
        }

        Claims c = jwtTokenProvider.getClaims(token);
        if (!"PW_RESET_LINK".equals(c.get("flowType", String.class))) {
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("링크가 만료되었거나 올바르지 않습니다."));
        }

        java.util.Map<String, Object> flow = flowClaimsOf(c);
        String userid = flow.get("userid") == null ? null : flow.get("userid").toString();
        String email = flow.get("email") == null ? null : flow.get("email").toString();

        if (userid == null || userid.isBlank() || email == null || email.isBlank()) {
            return redirectFrontWithQuery("/FindPassword", "error=" + enc("링크가 만료되었거나 올바르지 않습니다."));
        }

        // ✅ NewPassword 제출 시 검증할 쿠키(PW_RESET_TMP) 발급
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userid", userid);
        claims.put("email", email);

        String tmp = jwtTokenProvider.createFlowToken("PW_RESET", claims, 900);
        CookieUtils.addHttpOnlyCookie(request, response, "PW_RESET_TMP", tmp, 900, "Lax");

        return redirectFrontWithQuery("/NewPassword", "userid=" + enc(userid));
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
            return redirectFrontWithQuery("/NewPassword", "userid=" + enc(userid));
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
}