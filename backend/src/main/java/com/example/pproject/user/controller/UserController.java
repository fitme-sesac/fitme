package com.example.pproject.user.controller;

import com.example.pproject.Config.CookieUtils;
import com.example.pproject.Config.JwtTokenProvider;
import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.Constant.SocialType;
import com.example.pproject.user.dto.UserRequestDTO;
import com.example.pproject.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;
    private final JavaMailSender mailSender;
    private final JwtTokenProvider jwtTokenProvider;
    @Value("${verify.pepper}")
    private String verifyPepper;

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private String hashVerifyCode(String code) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest((verifyPepper + ":" + code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(dig);
        } catch (Exception e) {
            throw new IllegalStateException("verify code hash error", e);
        }
    }

    private boolean equalsHash(String hashB64Url, String code) {
        if (hashB64Url == null) return false;
        byte[] a = java.util.Base64.getUrlDecoder().decode(hashB64Url);
        byte[] b = java.util.Base64.getUrlDecoder().decode(hashVerifyCode(code));
        return java.security.MessageDigest.isEqual(a, b);
    }

    private String generateRandomCodeSecure6() {
        int n = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(n);
    }

    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    // ✅ 임시(또는 운영) 약관/개인정보/운영정책 notice_id (DB의 notice.notice_id)
    @Value("${app.notice.terms-id:0}")
    private Long termsNoticeId;

    @Value("${app.notice.privacy-id:0}")
    private Long privacyNoticeId;

    @Value("${app.notice.policy-id:0}")
    private Long policyNoticeId;

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

    private void applyPolicyNoticeIdsOrThrow(UserRequestDTO userDTO) {
        // DDL의 ck_member_required_consents 때문에 ACTIVE이면 최소 TERMS/PRIVACY notice_id가 필요
        if (termsNoticeId == null || termsNoticeId <= 0
                || privacyNoticeId == null || privacyNoticeId <= 0) {
            throw new IllegalStateException("약관 문서(TERMS/PRIVACY)가 준비되지 않았습니다. (notice_id 설정 필요)");
        }
        userDTO.setTermsNoticeId(termsNoticeId);
        userDTO.setPrivacyNoticeId(privacyNoticeId);

        // DDL에서는 필수 강제가 아니지만, 코드/정책 일관성 위해 세팅
        if (policyNoticeId != null && policyNoticeId > 0) {
            userDTO.setPolicyNoticeId(policyNoticeId);
        }
    }

    // 로그인 페이지 이동
    @GetMapping("/Login")
    public String login(Model model,
                        @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return redirectFrontWithQuery("/Login", errorMessage == null ? "" : ("errorMessage=" + enc(errorMessage)));
    }

    // 로그아웃은 Spring Security의 POST /Logout 처리로 일원화한다.

    @GetMapping("/User/Register")
    public String registerForm(Model model) {
        return redirectFront("/Register");
    }

    @PostMapping("/User/Register")
    public String registerProc(
            UserRequestDTO userDTO,
            @RequestParam("emailId") String emailId,
            @RequestParam("emailDomain") String emailDomain,
            @RequestParam("emailTLD") String emailTLD,
            HttpServletRequest request,
            HttpServletResponse response,
            @CookieValue(value = "PHONE_VERIFIED_TMP", required = false) String phoneVerifiedToken,
            Model model
    ) {
        try {
            // 1) 이메일 세 조각을 하나로 합치기
            String email = emailId.trim() + "@" + emailDomain.trim() + "." + emailTLD.trim();
            userDTO.setEmail(email);

            // 2) 필수 약관 동의(프론트 체크박스)
            if (!Boolean.TRUE.equals(userDTO.getAgreeTerms())
                    || !Boolean.TRUE.equals(userDTO.getAgreePrivacy())
                    || !Boolean.TRUE.equals(userDTO.getAgreePolicy())) {
                throw new IllegalStateException("필수 약관에 동의해야 가입이 가능합니다.");
            }

            // 3) 휴대폰 인증(서버 쿠키 기반)
            String normalizedPhone = userDTO.getPhone() == null ? "" : userDTO.getPhone().replaceAll("[^0-9]", "");
            String verifiedPhone = PhoneOtpController.readVerifiedPhone(jwtTokenProvider, phoneVerifiedToken, "SIGNUP");
            if (verifiedPhone == null || !verifiedPhone.equals(normalizedPhone)) {
                throw new IllegalStateException("휴대폰 인증을 완료해주세요.");
            }

            // 4) 서버가 결정하는 값들
            userDTO.setPhone(normalizedPhone);
            userDTO.setPhoneVerifiedAt(java.time.LocalDateTime.now());

            // ✅ notice_id + agreed_at 세팅 (DB 체크제약 통과)
            applyPolicyNoticeIdsOrThrow(userDTO);
            userDTO.setTermsAgreedAt(java.time.LocalDateTime.now());
            userDTO.setPrivacyAgreedAt(java.time.LocalDateTime.now());
            userDTO.setPolicyAgreedAt(java.time.LocalDateTime.now());

            if (Boolean.TRUE.equals(userDTO.getMarketingOptIn())) {
                userDTO.setMarketingAgreedAt(java.time.LocalDateTime.now());
            } else {
                userDTO.setMarketingOptIn(false);
                userDTO.setMarketingAgreedAt(null);
            }

            userDTO.setConsentIp(getClientIp(request));
            userDTO.setConsentUserAgent(request.getHeader("User-Agent"));

            // 5) 서비스에서 회원가입 처리
            userService.register(userDTO);

            CookieUtils.deleteCookie(request, response, "PHONE_VERIFIED_TMP");
        } catch (IllegalStateException e) {
            return redirectFrontWithQuery("/Register", "errorMessage=" + enc(e.getMessage()));
        }

        return redirectFront("/Login");
    }

    @GetMapping("/User/First_Social_Login")
    public String firstSocialLoginForm(HttpServletRequest request,
                                       @CookieValue(value = "OAUTH2_TMP", required = false) String tmpToken) {

        if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
            return redirectFrontWithQuery("/Login", "errorMessage=" + enc("소셜 가입 절차가 만료되었습니다. 다시 로그인해주세요."));
        }

        Claims c = jwtTokenProvider.getClaims(tmpToken);
        if (!"OAUTH2_REGISTER".equals(c.get("flowType", String.class))) {
            return redirectFrontWithQuery("/Login", "errorMessage=" + enc("소셜 가입 절차가 만료되었습니다. 다시 로그인해주세요."));
        }

        Object claimsObj = c.get("claims");
        Map<String, Object> flowClaims = Collections.emptyMap();
        if (claimsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tmp = (Map<String, Object>) claimsObj;
            flowClaims = tmp;
        }

        String email = flowClaims.get("email") == null ? "" : flowClaims.get("email").toString();
        String name = flowClaims.get("name") == null ? "" : flowClaims.get("name").toString();
        String provider = flowClaims.get("provider") == null ? "OTHER" : flowClaims.get("provider").toString();

        String q = "email=" + enc(email) + "&username=" + enc(name) + "&socialType=" + enc(provider);
        return redirectFrontWithQuery("/FirstSocialLogin", q);
    }

    @PostMapping("/User/First_Social_Login")
    public String firstSocialLoginSubmit(@ModelAttribute("data") UserRequestDTO userDTO,
                                         HttpServletRequest request,
                                         HttpServletResponse response,
                                         @CookieValue(value = "OAUTH2_TMP", required = false) String tmpToken,
                                         @CookieValue(value = "PHONE_VERIFIED_TMP", required = false) String phoneVerifiedToken) {
        try {
            // 1) OAuth2 임시 토큰 검증(이메일 위변조 방지)
            if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
                throw new IllegalStateException("소셜 가입 절차가 만료되었습니다. 다시 로그인해주세요.");
            }
            Claims c = jwtTokenProvider.getClaims(tmpToken);
            if (!"OAUTH2_REGISTER".equals(c.get("flowType", String.class))) {
                throw new IllegalStateException("소셜 가입 절차가 만료되었습니다. 다시 로그인해주세요.");
            }
            Object claimsObj = c.get("claims");
            Map<String, Object> flowClaims = Collections.emptyMap();
            if (claimsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tmp = (Map<String, Object>) claimsObj;
                flowClaims = tmp;
            }
            String emailFromOAuth = flowClaims.get("email") == null ? null : flowClaims.get("email").toString();
            if (emailFromOAuth == null || emailFromOAuth.isBlank()) {
                throw new IllegalStateException("소셜 가입 절차가 만료되었습니다. 다시 로그인해주세요.");
            }
            if (userDTO.getEmail() == null || !emailFromOAuth.equalsIgnoreCase(userDTO.getEmail())) {
                throw new IllegalStateException("이메일 정보가 일치하지 않습니다. 다시 로그인해주세요.");
            }

            // 2) 필수 약관 동의
            if (!Boolean.TRUE.equals(userDTO.getAgreeTerms())
                    || !Boolean.TRUE.equals(userDTO.getAgreePrivacy())
                    || !Boolean.TRUE.equals(userDTO.getAgreePolicy())) {
                throw new IllegalStateException("필수 약관에 동의해야 가입이 가능합니다.");
            }

            // 3) 휴대폰 인증(서버 쿠키 기반)
            String normalizedPhone = userDTO.getPhone() == null ? "" : userDTO.getPhone().replaceAll("[^0-9]", "");
            String verifiedPhone = PhoneOtpController.readVerifiedPhone(jwtTokenProvider, phoneVerifiedToken, "SIGNUP");
            if (verifiedPhone == null || !verifiedPhone.equals(normalizedPhone)) {
                throw new IllegalStateException("휴대폰 인증을 완료해주세요.");
            }


            // 4) 서버가 결정하는 값들
            String provider = flowClaims.get("provider") == null ? "OTHER" : flowClaims.get("provider").toString();
            userDTO.setSocialType(SocialType.from(provider));
            userDTO.setPhone(normalizedPhone);
            userDTO.setPhoneVerifiedAt(java.time.LocalDateTime.now());

            // ✅ notice_id + agreed_at 세팅
            applyPolicyNoticeIdsOrThrow(userDTO);
            userDTO.setTermsAgreedAt(java.time.LocalDateTime.now());
            userDTO.setPrivacyAgreedAt(java.time.LocalDateTime.now());
            userDTO.setPolicyAgreedAt(java.time.LocalDateTime.now());

            if (Boolean.TRUE.equals(userDTO.getMarketingOptIn())) {
                userDTO.setMarketingAgreedAt(java.time.LocalDateTime.now());
            } else {
                userDTO.setMarketingOptIn(false);
                userDTO.setMarketingAgreedAt(null);
            }
            userDTO.setConsentIp(getClientIp(request));
            userDTO.setConsentUserAgent(request.getHeader("User-Agent"));

            // 5) 가입 처리
            userService.register(userDTO);

            // 6) 가입 직후 로그인 처리(ACCESS_TOKEN 발급)
            var saved = userService.findByEmailOrThrow(userDTO.getEmail());
            var authForToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    saved.getUserid(),
                    null,
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + saved.getRoleType().name()))
            );
            String accessToken = jwtTokenProvider.createAccessToken(authForToken, saved.getUsername(), saved.getEmail());
            CookieUtils.addHttpOnlyCookie(request, response, "ACCESS_TOKEN", accessToken,
                    jwtTokenProvider.getAccessTokenValiditySeconds(), "Lax");

            CookieUtils.deleteCookie(request, response, "OAUTH2_TMP");
            CookieUtils.deleteCookie(request, response, "PHONE_VERIFIED_TMP");

            return redirectFront("/");
        } catch (IllegalStateException e) {
            return redirectFrontWithQuery("/FirstSocialLogin", "errorMessage=" + enc(e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // =========================
    // ✅ 아이디 찾기 (이메일 인증 방식)
    // =========================

    @GetMapping("/User/Find_Userid")
    public String findUseridForm() {
        return redirectFront("/FindUserId");
    }

    @PostMapping("/User/Find_Userid")
    public String sendUseridVerifyCode(@RequestParam String email,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        try {
            userService.assertEmailExists(email);

            String code = generateRandomCodeSecure6();
            String codeHash = hashVerifyCode(code);

            Map<String, Object> claims = new HashMap<>();
            claims.put("email", email);
            claims.put("codeHash", codeHash);
            claims.put("attempts", 0);

            String tmp = jwtTokenProvider.createFlowToken("FIND_USERID", claims, 600);
            CookieUtils.addHttpOnlyCookie(request, response, "FIND_USERID_TMP", tmp, 600, "Lax");

            // ✅ 이메일 발송
            sendVerificationEmail(email, code);

            return redirectFrontWithQuery("/VerifyUserIdCode", "email=" + enc(email));

        } catch (IllegalStateException e) {
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc(e.getMessage()));
        }
    }

    @PostMapping("/User/Verify_Userid_Code")
    public String verifyUseridCode(@RequestParam String inputCode,
                                   @CookieValue(value = "FIND_USERID_TMP", required = false) String tmpToken,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {

        if (tmpToken == null || tmpToken.isBlank() || !jwtTokenProvider.validateToken(tmpToken)) {
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        Claims c = jwtTokenProvider.getClaims(tmpToken);
        if (!"FIND_USERID".equals(c.get("flowType", String.class))) {
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        Map<String, Object> flowClaims = Collections.emptyMap();
        Object claimsObj = c.get("claims");
        if (claimsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tmp = (Map<String, Object>) claimsObj;
            flowClaims = tmp;
        }

        String email = flowClaims.get("email") == null ? null : flowClaims.get("email").toString();
        String codeHash = flowClaims.get("codeHash") == null ? null : flowClaims.get("codeHash").toString();
        Integer attempts = null;
        Object at = flowClaims.get("attempts");
        if (at instanceof Number n) attempts = n.intValue();
        if (attempts == null) attempts = 0;

        if (email == null || codeHash == null) {
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        // ✅ 5회 제한
        if (attempts >= 5) {
            CookieUtils.deleteCookie(request, response, "FIND_USERID_TMP");
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc("인증 실패 횟수를 초과했습니다. 다시 시도해주세요."));
        }

        // ✅ 인증 성공 → 아이디 조회
        String userid = userService.findUseridByEmail(email);

        // ✅ 플로우 쿠키 정리
        CookieUtils.deleteCookie(request, response, "FIND_USERID_TMP");
        return redirectFrontWithQuery("/ResultUserId", "message=" + enc("당신의 아이디는: " + userid));
    }

    private void sendVerificationEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("아이디 찾기 인증번호");
        message.setText("인증번호: " + code);
        mailSender.send(message);
    }

    // =========================
    // 기존 비밀번호 변경 (로그인 상태)
    // =========================

    @GetMapping("/User/Change_Password")
    public String showChangePasswordPage() {
        return redirectFront("/ChangePassword");
    }

    @PostMapping("/User/Change_Password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 @AuthenticationPrincipal JwtUserPrincipal principal,
                                 Model model) {
        if (principal == null) return redirectFrontWithQuery("/Login", "errorMessage=" + enc("로그인이 필요합니다."));

        String userid = principal.getUserid();

        if (!userService.verifyPassword(userid, currentPassword)) {
            return redirectFrontWithQuery("/ChangePassword", "errorMessage=" + enc("현재 비밀번호가 일치하지 않습니다."));
        }

        if (!newPassword.equals(confirmPassword)) {
            return redirectFrontWithQuery("/ChangePassword", "errorMessage=" + enc("새 비밀번호와 비밀번호 확인이 일치하지 않습니다."));
        }

        userService.updatePassword(userid, newPassword);
        return redirectFront("/");
    }
}