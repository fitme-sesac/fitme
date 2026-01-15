// src/main/java/com/example/pproject/user/controller/UserController.java
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

    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    // ✅ 임시(또는 운영) 약관/개인정보/운영정책 notice_id (DB의 notice.notice_id)
    @Value("${app.notice.terms-id:0}")
    private Long termsNoticeId;

    @Value("${app.notice.privacy-id:0}")
    private Long privacyNoticeId;

    @Value("${app.notice.policy-id:0}")
    private Long policyNoticeId;

    /**
     * Builds a Spring MVC redirect view string that points to the configured front-end base URL plus the provided path.
     *
     * @param path the path segment to append to the front-end base URL (appended as-is)
     * @return a redirect view string combining the "redirect:" prefix, the front-end base URL, and the provided path
     */
    private String redirectFront(String path) {
        return "redirect:" + frontBaseUrl + path;
    }

    private String redirectFrontWithQuery(String path, String query) {
        if (query == null || query.isBlank()) return redirectFront(path);
        return "redirect:" + frontBaseUrl + path + "?" + query;
    }

    /**
     * URL-encodes the given string using UTF-8 and returns an empty string if the input is null.
     *
     * @param v the string to URL-encode; may be null
     * @return the UTF-8 URL-encoded representation of {@code v}, or an empty string if {@code v} is null
     */
    private String enc(String v) {
        return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    /**
     * Ensures required notice IDs are configured and applies them to the given user DTO.
     *
     * Sets the terms and privacy notice IDs on the provided UserRequestDTO and sets the policy
     * notice ID when available (> 0).
     *
     * @param userDTO the user registration DTO to populate with notice IDs
     * @throws IllegalStateException if the terms or privacy notice ID is missing or not greater than zero
     */
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

    /**
     * Redirects the user to the front-end login page, including an optional encoded error message.
     *
     * @param errorMessage an optional error message to display on the login page; ignored if null or blank
     * @return the redirect URL to the front-end login page, including an `errorMessage` query parameter when provided
     */
    @GetMapping("/Login")
    public String login(Model model,
                        @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return redirectFrontWithQuery("/Login", errorMessage == null ? "" : ("errorMessage=" + enc(errorMessage)));
    }

    /**
     * Redirects the request to the front-end registration page.
     *
     * @return a redirect view string pointing to the front-end "/Register" path
     */

    @GetMapping("/User/Register")
    public String registerForm(Model model) {
        return redirectFront("/Register");
    }

    /**
     * Handle user registration form submission: validate required consents, verify phone via the temporary phone token,
     * apply configured policy/notice IDs, set consent metadata, register the user, and redirect to the appropriate front-end page.
     *
     * @param userDTO            DTO containing user-provided registration fields (expects email will be set from the emailId/emailDomain/emailTLD parts,
     *                           and uses fields such as `agreeTerms`, `agreePrivacy`, `agreePolicy`, `phone`, and `marketingOptIn`)
     * @param emailId            local-part of the email address (combined with {@code emailDomain} and {@code emailTLD})
     * @param emailDomain        domain of the email address (combined with {@code emailId} and {@code emailTLD})
     * @param emailTLD           top-level domain of the email address (combined with {@code emailId} and {@code emailDomain})
     * @param request            HTTP request (used to obtain client IP and User-Agent for consent metadata)
     * @param response           HTTP response (used to delete the phone verification cookie on success)
     * @param phoneVerifiedToken optional cookie token ("PHONE_VERIFIED_TMP") containing phone verification claims used to validate the submitted phone number
     * @param model              MVC model (preserved for controller compatibility; not required for primary processing)
     * @return a redirect URL: on success redirects to the front-end Login page; on validation or verification failure redirects to the front-end Register page
     *         with an encoded {@code errorMessage} query parameter describing the failure.
     */
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
            String verifiedPhone = PhoneOtpController.readVerifiedPhone(jwtTokenProvider, phoneVerifiedToken);
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

    /**
     * Redirects the user to the front-end first-social-login page with email and username extracted from a temporary OAuth2 token.
     *
     * @param tmpToken the value of the `OAUTH2_TMP` cookie containing a temporary OAuth2 flow token; may be null or invalid
     * @return a redirect URL string to the front-end `/FirstSocialLogin` with `email` and `username` query parameters;
     *         if the token is missing, invalid, or not associated with the `OAUTH2_REGISTER` flow, a redirect URL to the front-end `/Login`
     *         containing an `errorMessage` query parameter is returned
     */
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

        String q = "email=" + enc(email) + "&username=" + enc(name);
        return redirectFrontWithQuery("/FirstSocialLogin", q);
    }

    /**
     * Completes first-time social registration: validates OAuth2 and phone verification, applies required notices and consents,
     * registers the user, issues an access token cookie, and redirects to the front-end root on success.
     *
     * @param userDTO               incoming user registration data from the social-first form (email, phone, consent flags, etc.)
     * @param request               current HTTP request (used to obtain client IP and headers)
     * @param response              current HTTP response (used to set and clear cookies)
     * @param tmpToken              optional OAUTH2_TMP cookie containing a short-lived OAuth2 flow token for email verification
     * @param phoneVerifiedToken    optional PHONE_VERIFIED_TMP cookie containing a short-lived phone verification token
     * @return                      a redirect URL string to send the client to the appropriate front-end page (success or form with error)
     */
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
            String verifiedPhone = PhoneOtpController.readVerifiedPhone(jwtTokenProvider, phoneVerifiedToken);
            if (verifiedPhone == null || !verifiedPhone.equals(normalizedPhone)) {
                throw new IllegalStateException("휴대폰 인증을 완료해주세요.");
            }

            // 4) 서버가 결정하는 값들
            userDTO.setSocialType(SocialType.GOOGLE);
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
    /**
     * Redirects the request to the front-end FindUserId page.
     *
     * @return the redirect URL to the front-end path "/FindUserId"
     */

    @GetMapping("/User/Find_Userid")
    public String findUseridForm() {
        return redirectFront("/FindUserId");
    }

    /**
     * Initiates the "find userid" flow by generating a one-time verification code, emailing it to the given address,
     * and storing a short-lived flow token in an HttpOnly cookie.
     *
     * @param email the recipient email address to verify
     * @param model spring MVC model (unused in success path)
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @return a redirect URL to the verification-code page with the email query on success; on failure, a redirect to
     *         the FindUserId page with an encoded `errorMessage` query parameter
     */
    @PostMapping("/User/Find_Userid")
    public String sendUseridVerifyCode(@RequestParam String email,
                                       Model model,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        try {
            userService.assertEmailExists(email);

            String code = generateRandomCode();

            Map<String, Object> claims = new HashMap<>();
            claims.put("email", email);
            claims.put("code", code);

            String tmp = jwtTokenProvider.createFlowToken("FIND_USERID", claims, 600);
            CookieUtils.addHttpOnlyCookie(request, response, "FIND_USERID_TMP", tmp, 600, "Lax");

            sendVerificationEmail(email, code);

            return redirectFrontWithQuery("/VerifyUserIdCode", "email=" + enc(email));

        } catch (IllegalStateException e) {
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc(e.getMessage()));
        }
    }

    /**
     * Verifies a one-time code for the "find userid" flow and redirects to the appropriate front-end page.
     *
     * Validates the temporary FIND_USERID flow token and the submitted code, clears the temporary cookie, and
     * when valid redirects to the result page containing the recovered userid. If the token is missing/invalid/expired
     * or the code does not match, redirects back to the appropriate entry page with an error message.
     *
     * @param inputCode the verification code submitted by the user
     * @param tmpToken  the value of the FIND_USERID_TMP cookie (may be null or empty)
     * @return a redirect URL to the front-end: on success to /ResultUserId with the userid message; on code mismatch to /VerifyUserIdCode with an error; on expired/invalid flow to /FindUserId with an error
     */
    @PostMapping("/User/Verify_Userid_Code")
    public String verifyUseridCode(@RequestParam String inputCode,
                                   Model model,
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

        String savedCode = flowClaims.get("code") == null ? null : flowClaims.get("code").toString();
        String email = flowClaims.get("email") == null ? null : flowClaims.get("email").toString();

        if (savedCode == null || email == null) {
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        if (!inputCode.equals(savedCode)) {
            return redirectFrontWithQuery("/VerifyUserIdCode", "errorMessage=" + enc("인증번호가 일치하지 않습니다."));
        }

        String userid = userService.findUseridByEmail(email);

        CookieUtils.deleteCookie(request, response, "FIND_USERID_TMP");

        return redirectFrontWithQuery("/ResultUserId", "message=" + enc("당신의 아이디는: " + userid));
    }

    private String generateRandomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1_000_000));
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

    /**
     * Handle an authenticated user's request to change their password and redirect according to validation outcome.
     *
     * @param currentPassword   the user's current password for verification
     * @param newPassword       the new password to set
     * @param confirmPassword   confirmation of the new password; must match {@code newPassword}
     * @param principal         the authenticated user's principal; if null the user is considered unauthenticated
     * @param model             the Spring MVC model (used for view attributes)
     * @return                  a redirect URL string to the appropriate front-end path: redirects to the login page if unauthenticated, back to the change-password page with an error message on validation failure, or to the front-end root on success
     */
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