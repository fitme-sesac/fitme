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
import java.time.Instant;
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
        if (hashB64Url == null)
            return false;
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
        if (query == null || query.isBlank())
            return redirectFront(path);
        return "redirect:" + frontBaseUrl + path + "?" + query;
    }

    private String enc(String v) {
        return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

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
    // =========================
    private String str(Object o) {
        return (o == null) ? "" : String.valueOf(o).trim();
    }

    private String normalizePhoneDigits(String v) {
        if (v == null)
            return "";
        return v.replaceAll("[^0-9]", "");
    }

    private String normalizeGender(String g) {
        if (g == null)
            return "";
        String v = g.trim().toUpperCase();
        if (v.isBlank())
            return "";
        return switch (v) {
            case "M", "MALE" -> "MALE";
            case "F", "FEMALE" -> "FEMALE";
            case "U", "UNDISCLOSED" -> "UNDISCLOSED";
            default -> "";
        };
    }

    // ✅ birthday는 이제 기본적으로 YYYY-MM-DD를 받는다 (NAVER 정규화)
    // ✅ 예전 호환: birthyear="1994" + birthday="12-31" 조합도 처리
    private String normalizeBirthDate(String yyyyMmDdOrEmpty, String birthyear, String mmDd) {
        String v = (yyyyMmDdOrEmpty == null) ? "" : yyyyMmDdOrEmpty.trim();
        if (!v.isBlank() && v.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))
            return v;

        if (birthyear == null || mmDd == null)
            return "";
        String y = birthyear.trim();
        String bd = mmDd.trim();
        if (y.isBlank() || bd.isBlank())
            return "";
        if (!y.matches("^[0-9]{4}$"))
            return "";
        if (!bd.matches("^[0-9]{2}-[0-9]{2}$"))
            return "";
        return y + "-" + bd;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public String login(Model model,
            @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("errorMessage", errorMessage);
        }

        String q = "tab=login";
        if (errorMessage != null && !errorMessage.isBlank()) {
            q += "&errorMessage=" + enc(errorMessage);
        }
        return redirectFrontWithQuery("/auth", q);
    }

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
            Model model) {
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
            userDTO.setPhoneVerifiedAt(Instant.now());

            applyPolicyNoticeIdsOrThrow(userDTO);
            userDTO.setTermsAgreedAt(Instant.now());
            userDTO.setPrivacyAgreedAt(Instant.now());
            userDTO.setPolicyAgreedAt(Instant.now());

            if (Boolean.TRUE.equals(userDTO.getMarketingOptIn())) {
                userDTO.setMarketingAgreedAt(Instant.now());
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

    // ✅ 소셜 최초 가입 화면 이동 (OAUTH2_TMP -> 프론트 쿼리 전달)
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
        if (provider.isBlank())
            provider = "OTHER";

        // ✅ 이름(name) = 실명/이름 (닉네임 X)
        String username = str(flowClaims.get("name"));

        // ✅ 통일 키 우선 사용
        String gender = normalizeGender(str(flowClaims.get("gender"))); // MALE/FEMALE/UNDISCLOSED
        String birthdayRaw = str(flowClaims.get("birthday")); // YYYY-MM-DD 기대
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
        if (!username.isBlank())
            q.append("&username=").append(enc(username));
        if (!gender.isBlank())
            q.append("&gender=").append(enc(gender));
        if (!birthdayRaw.isBlank())
            q.append("&birthday=").append(enc(birthdayRaw));
        if (!phoneDigits.isBlank())
            q.append("&phone=").append(enc(phoneDigits));

        return redirectFrontWithQuery("/FirstSocialLogin", q.toString());
    }

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
            userDTO.setPhoneVerifiedAt(Instant.now());

            applyPolicyNoticeIdsOrThrow(userDTO);
            userDTO.setTermsAgreedAt(Instant.now());
            userDTO.setPrivacyAgreedAt(Instant.now());
            userDTO.setPolicyAgreedAt(Instant.now());

            if (Boolean.TRUE.equals(userDTO.getMarketingOptIn())) {
                userDTO.setMarketingAgreedAt(Instant.now());
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
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            "ROLE_" + saved.getRoleType().name())));
            String accessToken = jwtTokenProvider.createAccessToken(authForToken, saved.getUsername(),
                    saved.getEmail());
            CookieUtils.addHttpOnlyCookie(request, response, "ACCESS_TOKEN", accessToken,
                    jwtTokenProvider.getAccessTokenValiditySeconds(), "Lax");

            CookieUtils.deleteCookie(request, response, "OAUTH2_TMP");
            CookieUtils.deleteCookie(request, response, "PHONE_VERIFIED_TMP");

            return redirectFront("/");
        } catch (IllegalStateException e) {
            return redirectFrontWithQuery("/FirstSocialLogin", "errorMessage=" + enc(e.getMessage()));
        }
    }

    // ===== 아이디 찾기 =====
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
        Integer attempts = 0;
        Object at = flowClaims.get("attempts");
        if (at instanceof Number n)
            attempts = n.intValue();

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

    private void sendVerificationEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("아이디 찾기 인증번호");
        message.setText("인증번호: " + code);
        mailSender.send(message);
    }

    // ===== 비밀번호 변경(로그인 상태) =====
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
        if (principal == null)
            return redirectFrontWithQuery("/Login", "errorMessage=" + enc("로그인이 필요합니다."));

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
