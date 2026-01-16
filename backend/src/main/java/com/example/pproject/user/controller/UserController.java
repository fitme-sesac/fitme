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

    // ✅ 누락되면 컴파일 터짐
    private final PhoneOtpController phoneOtpController; // 필요 없으면 제거 가능 (하지만 보통 컨트롤러 빈 충돌 방지용)

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

    /**
     * URL-encodes the given string using UTF-8, returning an empty string if the input is null.
     *
     * @param v the string to URL-encode; may be null
     * @return the UTF-8 URL-encoded string, or an empty string if {@code v} is null
     */
    private String enc(String v) {
        return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    /**
     * Apply configured notice IDs for terms, privacy, and the optional policy to the given user request DTO.
     *
     * @param userDTO the user request DTO to populate with notice IDs
     * @throws IllegalStateException if required terms or privacy notice IDs are not configured (null or <= 0)
     */
    private void applyPolicyNoticeIdsOrThrow(UserRequestDTO userDTO) {
        if (termsNoticeId == null || termsNoticeId <= 0
                || privacyNoticeId == null || privacyNoticeId <= 0) {
            throw new IllegalStateException("약관 문서(TERMS/PRIVACY)가 준비되지 않았습니다. (notice_id 설정 필요)");
        }
        userDTO.setTermsNoticeId(termsNoticeId);
        userDTO.setPrivacyNoticeId(privacyNoticeId);

        if (policyNoticeId != null && policyNoticeId > 0) {
            userDTO.setPolicyNoticeId(policyNoticeId);
        }
    }

    // =========================
    // ✅ 공통 유틸 (str 해결)
    /**
     * Convert an object to a trimmed string, returning an empty string for null.
     *
     * @param o the object to convert; may be null
     * @return the trimmed string representation of {@code o}, or an empty string if {@code o} is null
     */
    private String str(Object o) {
        return (o == null) ? "" : String.valueOf(o).trim();
    }

    /**
     * Normalize a phone string by removing all non-digit characters.
     *
     * @param v the input phone string (may contain spaces, punctuation, or country prefixes)
     * @return  a string containing only the digits from `v`, or an empty string if `v` is null
     */
    private String normalizePhoneDigits(String v) {
        if (v == null) return "";
        return v.replaceAll("[^0-9]", "");
    }

    /**
     * Normalize a gender input string to a canonical value.
     *
     * @param g the input gender value (may be null, empty, or various case/format variants)
     * @return "MALE", "FEMALE", or "UNDISCLOSED" when the input maps to one of those values; an empty string otherwise
     */
    private String normalizeGender(String g) {
        if (g == null) return "";
        String v = g.trim().toUpperCase();
        if (v.isBlank()) return "";
        return switch (v) {
            case "M", "MALE" -> "MALE";
            case "F", "FEMALE" -> "FEMALE";
            case "U", "UNDISCLOSED" -> "UNDISCLOSED";
            default -> "";
        };
    }

    // ✅ birthday는 이제 기본적으로 YYYY-MM-DD를 받는다 (NAVER 정규화)
    /**
     * Normalizes birth date input into a YYYY-MM-DD string.
     *
     * Accepts either a full date in the form "YYYY-MM-DD" or a legacy two-field form
     * where `birthyear` is "YYYY" and `mmDd` is "MM-DD". If the inputs do not match
     * these formats or are missing/blank, returns an empty string.
     *
     * @param yyyyMmDdOrEmpty a full birth date in "YYYY-MM-DD" format, or blank/empty
     * @param birthyear a four-digit year ("YYYY") used with `mmDd` for legacy input
     * @param mmDd a month-day pair in "MM-DD" format used with `birthyear` for legacy input
     * @return a normalized date in "YYYY-MM-DD" format, or an empty string if inputs are invalid
     */
    private String normalizeBirthDate(String yyyyMmDdOrEmpty, String birthyear, String mmDd) {
        String v = (yyyyMmDdOrEmpty == null) ? "" : yyyyMmDdOrEmpty.trim();
        if (!v.isBlank() && v.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) return v;

        if (birthyear == null || mmDd == null) return "";
        String y = birthyear.trim();
        String bd = mmDd.trim();
        if (y.isBlank() || bd.isBlank()) return "";
        if (!y.matches("^[0-9]{4}$")) return "";
        if (!bd.matches("^[0-9]{2}-[0-9]{2}$")) return "";
        return y + "-" + bd;
    }

    /**
     * Determines the client's IP address, preferring the first value from the `X-Forwarded-For` header when present.
     *
     * @param request the HTTP servlet request from which to extract the client IP
     * @return the client's IP address as a string; if `X-Forwarded-For` is present, the first comma-separated value is returned, otherwise the request's remote address
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Redirects to the front-end Login page, preserving an optional error message.
     *
     * @param model        Spring MVC model used to expose the error message to the view when present
     * @param errorMessage optional error message to include as the `errorMessage` query parameter; null or blank values are omitted
     * @return             a redirect URL to the front-end Login page; includes the `errorMessage` query parameter when provided
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
     * Redirects to the front-end registration page.
     *
     * @return the redirect URL to the front-end "/Register" path
     */
    @GetMapping("/User/Register")
    public String registerForm(Model model) {
        return redirectFront("/Register");
    }

    /**
     * Handle registration form submission: validate input, verify phone, record consents, create the user, and clear the phone-verification cookie.
     *
     * @param userDTO            DTO carrying registration fields (username, password, phone, agreement flags, etc.)
     * @param emailId            local-part of the user's email (will be combined with emailDomain and emailTLD)
     * @param emailDomain        domain part of the user's email (will be combined with emailId and emailTLD)
     * @param emailTLD           top-level domain of the user's email (will be combined with emailId and emailDomain)
     * @param request             HTTP request (used to capture client IP and User-Agent)
     * @param response            HTTP response (used to clear the PHONE_VERIFIED_TMP cookie)
     * @param phoneVerifiedToken  optional PHONE_VERIFIED_TMP cookie token containing the prior phone verification flow
     * @param model               MVC model (unused for success flow; preserved for controller signature)
     * @return                    redirect URL: front-end Login page on successful registration; front-end Register page with an `errorMessage` query parameter when validation or verification fails.
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
            String email = emailId.trim() + "@" + emailDomain.trim() + "." + emailTLD.trim();
            userDTO.setEmail(email);

            if (!Boolean.TRUE.equals(userDTO.getAgreeTerms())
                    || !Boolean.TRUE.equals(userDTO.getAgreePrivacy())
                    || !Boolean.TRUE.equals(userDTO.getAgreePolicy())) {
                throw new IllegalStateException("필수 약관에 동의해야 가입이 가능합니다.");
            }

            String normalizedPhone = normalizePhoneDigits(userDTO.getPhone());
            String verifiedPhone = PhoneOtpController.readVerifiedPhone(jwtTokenProvider, phoneVerifiedToken, "SIGNUP");
            if (verifiedPhone == null || !verifiedPhone.equals(normalizedPhone)) {
                throw new IllegalStateException("휴대폰 인증을 완료해주세요.");
            }

            userDTO.setPhone(normalizedPhone);
            userDTO.setPhoneVerifiedAt(java.time.LocalDateTime.now());

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

            userService.register(userDTO);

            CookieUtils.deleteCookie(request, response, "PHONE_VERIFIED_TMP");
        } catch (IllegalStateException e) {
            return redirectFrontWithQuery("/Register", "errorMessage=" + enc(e.getMessage()));
        }

        return redirectFront("/Login");
    }

    /**
     * Prepare social-first-signup data from a temporary OAuth2 flow token and redirect the user to the front-end first social login page.
     *
     * Extracts social identity claims (email, provider, name, gender, birthday, phone) from the `OAUTH2_TMP` JWT cookie, normalizes those values, and builds a query string to prefill the front-end /FirstSocialLogin page. If the temporary token is missing, invalid, or not an OAuth2 registration flow, redirects to the front-end /Login with an error message.
     *
     * @param tmpToken the temporary OAuth2 flow JWT from the `OAUTH2_TMP` cookie containing social claims; may be null or invalid
     * @return a redirect URL to the front-end /FirstSocialLogin with populated query parameters, or to /Login with an error message when the token is absent/invalid or not for registration
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

        String email = str(flowClaims.get("email"));
        String provider = str(flowClaims.get("provider"));
        if (provider.isBlank()) provider = "OTHER";

        // ✅ 이름(name) = 실명/이름 (닉네임 X)
        String username = str(flowClaims.get("name"));

        // ✅ 통일 키 우선 사용
        String gender = normalizeGender(str(flowClaims.get("gender")));         // MALE/FEMALE/UNDISCLOSED
        String birthdayRaw = str(flowClaims.get("birthday"));                   // YYYY-MM-DD 기대
        String phoneDigits = normalizePhoneDigits(str(flowClaims.get("phone"))); // digits 기대

        // ✅ 호환 키(과거/실수 대비)
        String birthyear = str(flowClaims.get("birthyear"));
        String mmdd = str(flowClaims.get("birthday_md")); // (사용 안하면 그냥 빈값)
        if (birthdayRaw.isBlank() && !birthyear.isBlank() && !mmdd.isBlank()) {
            birthdayRaw = normalizeBirthDate("", birthyear, mmdd);
        } else {
            birthdayRaw = normalizeBirthDate(birthdayRaw, birthyear, mmdd);
        }

        if (phoneDigits.isBlank()) {
            phoneDigits = normalizePhoneDigits(str(flowClaims.get("mobile")));
        }

        StringBuilder q = new StringBuilder();
        q.append("email=").append(enc(email));
        q.append("&socialType=").append(enc(provider));
        if (!username.isBlank()) q.append("&username=").append(enc(username));
        if (!gender.isBlank()) q.append("&gender=").append(enc(gender));
        if (!birthdayRaw.isBlank()) q.append("&birthday=").append(enc(birthdayRaw));
        if (!phoneDigits.isBlank()) q.append("&phone=").append(enc(phoneDigits));

        return redirectFrontWithQuery("/FirstSocialLogin", q.toString());
    }

    /**
     * Completes a social signup by validating the temporary OAuth2 flow token and phone verification, registering the user, issuing an access token cookie, and redirecting to the front-end root.
     *
     * @param userDTO            the user registration submission populated from the front-end form
     * @param request            the current HttpServletRequest (used to capture client IP and headers)
     * @param response           the current HttpServletResponse (used to set and delete cookies)
     * @param tmpToken           the OAUTH2_TMP cookie value containing the OAuth2 flow claims
     * @param phoneVerifiedToken the PHONE_VERIFIED_TMP cookie value used to confirm phone verification
     * @return                   a redirect URL to the front-end root on success, or a redirect to /FirstSocialLogin with an `errorMessage` query parameter on failure
     */
    @PostMapping("/User/First_Social_Login")
    public String firstSocialLoginSubmit(@ModelAttribute("data") UserRequestDTO userDTO,
                                         HttpServletRequest request,
                                         HttpServletResponse response,
                                         @CookieValue(value = "OAUTH2_TMP", required = false) String tmpToken,
                                         @CookieValue(value = "PHONE_VERIFIED_TMP", required = false) String phoneVerifiedToken) {
        try {
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

            if (!Boolean.TRUE.equals(userDTO.getAgreeTerms())
                    || !Boolean.TRUE.equals(userDTO.getAgreePrivacy())
                    || !Boolean.TRUE.equals(userDTO.getAgreePolicy())) {
                throw new IllegalStateException("필수 약관에 동의해야 가입이 가능합니다.");
            }

            String normalizedPhone = normalizePhoneDigits(userDTO.getPhone());
            String verifiedPhone = PhoneOtpController.readVerifiedPhone(jwtTokenProvider, phoneVerifiedToken, "SIGNUP");
            if (verifiedPhone == null || !verifiedPhone.equals(normalizedPhone)) {
                throw new IllegalStateException("휴대폰 인증을 완료해주세요.");
            }

            String provider = flowClaims.get("provider") == null ? "OTHER" : flowClaims.get("provider").toString();
            userDTO.setSocialType(SocialType.from(provider));
            userDTO.setPhone(normalizedPhone);
            userDTO.setPhoneVerifiedAt(java.time.LocalDateTime.now());

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

            userService.register(userDTO);

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

    /**
     * Redirects the client to the front-end FindUserId page.
     *
     * @return a redirect URL to the front-end "/FindUserId" page
     */
    @GetMapping("/User/Find_Userid")
    public String findUseridForm() {
        return redirectFront("/FindUserId");
    }

    /**
     * Initiates a userid recovery flow by emailing a 6-digit verification code to the given email.
     *
     * Creates a temporary flow token (600 seconds) containing the email, a hash of the code,
     * and an attempts counter set to 0, stores it in a HttpOnly cookie, and sends the code
     * to the recipient. On success redirects the user to the VerifyUserId page with the email
     * as a query parameter; on validation failure redirects back to the FindUserId page with
     * an error message.
     *
     * @param email   the recipient email address to which the verification code will be sent
     * @param request the current HTTP request (used to set the cookie)
     * @param response the current HTTP response (used to set the cookie)
     * @return a redirect URL to the front-end VerifyUserId page on success, or to FindUserId with an error on failure
     */
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

            sendVerificationEmail(email, code);

            return redirectFrontWithQuery("/VerifyUserIdCode", "email=" + enc(email));
        } catch (IllegalStateException e) {
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc(e.getMessage()));
        }
    }

    /**
     * Verifies a one-time code submitted for user ID recovery and redirects to the next front-end page.
     *
     * Validates the temporary FIND_USERID flow token, enforces a maximum of 5 attempts, compares the submitted
     * code to the stored hashed code, and on success returns a redirect to the result page containing the found userid.
     *
     * @param inputCode the verification code submitted by the user
     * @param tmpToken  the temporary flow JWT from the FIND_USERID_TMP cookie (may be null or invalid)
     * @param request   the current HTTP request (used for cookie operations)
     * @param response  the current HTTP response (used for adding/removing cookies)
     * @return a redirect URL string to the appropriate front-end route (error, verification retry, or result page)
     */
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
        Integer attempts = 0;
        Object at = flowClaims.get("attempts");
        if (at instanceof Number n) attempts = n.intValue();

        if (email == null || codeHash == null) {
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc("인증 절차가 만료되었습니다. 다시 시도해주세요."));
        }

        if (attempts >= 5) {
            CookieUtils.deleteCookie(request, response, "FIND_USERID_TMP");
            return redirectFrontWithQuery("/FindUserId", "errorMessage=" + enc("인증 실패 횟수를 초과했습니다. 다시 시도해주세요."));
        }

        if (!equalsHash(codeHash, inputCode)) {
            flowClaims = new HashMap<>(flowClaims);
            flowClaims.put("attempts", attempts + 1);
            String tmp = jwtTokenProvider.createFlowToken("FIND_USERID", flowClaims, 600);
            CookieUtils.addHttpOnlyCookie(request, response, "FIND_USERID_TMP", tmp, 600, "Lax");
            return redirectFrontWithQuery("/VerifyUserIdCode", "errorMessage=" + enc("인증번호가 일치하지 않습니다."));
        }

        String userid = userService.findUseridByEmail(email);
        CookieUtils.deleteCookie(request, response, "FIND_USERID_TMP");
        return redirectFrontWithQuery("/ResultUserId", "message=" + enc("당신의 아이디는: " + userid));
    }

    /**
     * Sends a verification email containing a one-time code for user ID recovery.
     *
     * @param to   the recipient email address
     * @param code the verification code to include in the message
     */
    private void sendVerificationEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("아이디 찾기 인증번호");
        message.setText("인증번호: " + code);
        mailSender.send(message);
    }

    /**
     * Redirects the client to the front-end Change Password page.
     *
     * @return the redirect URL pointing to the front-end "/ChangePassword" path
     */
    @GetMapping("/User/Change_Password")
    public String showChangePasswordPage() {
        return redirectFront("/ChangePassword");
    }

    /**
     * Updates the authenticated user's password when the provided current password is correct
     * and the new password matches its confirmation.
     *
     * If the caller is not authenticated, redirects to the login page. If the current password
     * is incorrect or the new password and confirmation do not match, redirects back to the
     * change-password page with an error message. On success, updates the user's password and
     * redirects to the front-page root.
     *
     * @param currentPassword the user's current password for verification
     * @param newPassword the new password to set
     * @param confirmPassword confirmation of the new password (must equal {@code newPassword})
     * @param principal the authenticated user principal; when null the flow redirects to login
     * @return a redirect URL string directing the client to the appropriate front-end page
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