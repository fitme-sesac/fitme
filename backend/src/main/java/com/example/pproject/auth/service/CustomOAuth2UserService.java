package com.example.pproject.auth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User origin = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> mapped = new LinkedHashMap<>();

        if ("naver".equalsIgnoreCase(registrationId)) {
            mapped.putAll(mapNaver(origin.getAttributes()));
        } else if ("kakao".equalsIgnoreCase(registrationId)) {
            mapped.putAll(mapKakao(origin.getAttributes()));
        } else if ("google".equalsIgnoreCase(registrationId)) {
            mapped.putAll(mapGoogle(origin.getAttributes()));
        } else {
            mapped.putAll(origin.getAttributes());
        }

        String configuredNameKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        String nameKey = chooseNameKey(mapped, configuredNameKey);

        return new DefaultOAuth2User(origin.getAuthorities(), mapped, nameKey);
    }

    private static String chooseNameKey(Map<String, Object> attributes, String preferred) {
        if (preferred != null && attributes.containsKey(preferred)) return preferred;
        if (attributes.containsKey("id")) return "id";
        if (attributes.containsKey("sub")) return "sub";
        if (attributes.containsKey("email")) return "email";
        return attributes.keySet().stream().findFirst().orElse("id");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapNaver(Map<String, Object> attributes) {
        Object responseObj = attributes.get("response");
        if (!(responseObj instanceof Map)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_naver_response"),
                    "NAVER userinfo response missing 'response'"
            );
        }

        Map<String, Object> response = (Map<String, Object>) responseObj;

        String id = asString(response.get("id"));
        if (id == null || id.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_naver_response"),
                    "NAVER userinfo response missing 'id'"
            );
        }

        String email = asString(response.get("email"));
        String name = asString(response.get("name")); // ✅ 실명 (동의/설정에 따라 null 가능)

        // NAVER 확장 필드
        String gender = normalizeNaverGender(asString(response.get("gender"))); // M/F/U -> MALE/FEMALE/UNDISCLOSED
        String birthyear = asString(response.get("birthyear"));                // YYYY
        String birthdayMd = asString(response.get("birthday"));                // MM-DD
        String birthday = combineBirthDate(birthyear, birthdayMd);             // YYYY-MM-DD
        String phone = normalizePhone(asString(response.get("mobile")));       // digits

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("id", id);
        mapped.put("provider", "NAVER");

        mapped.put("email", email);
        mapped.put("name", name);

        // ✅ 여기 3개를 “통일 키”로 씀 (SuccessHandler/UserController/프론트 모두 동일하게 사용)
        mapped.put("gender", gender);     // MALE/FEMALE/UNDISCLOSED/null
        mapped.put("birthday", birthday); // YYYY-MM-DD/null
        mapped.put("phone", phone);       // digits/null

        mapped.put("raw", response);
        return mapped;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapKakao(Map<String, Object> attributes) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("provider", "KAKAO");

        Object idObj = attributes.get("id");
        mapped.put("id", idObj == null ? null : String.valueOf(idObj));

        Object accountObj = attributes.get("kakao_account");
        if (accountObj instanceof Map) {
            Map<String, Object> account = (Map<String, Object>) accountObj;
            mapped.put("email", asString(account.get("email")));
        }

        // ✅ 닉네임 = 이름 아님 → name은 null로 둔다
        mapped.put("name", null);

        mapped.put("raw", attributes);
        return mapped;
    }

    private static Map<String, Object> mapGoogle(Map<String, Object> attributes) {
        Map<String, Object> mapped = new LinkedHashMap<>(attributes);
        mapped.put("provider", "GOOGLE");
        if (attributes.get("sub") != null) {
            mapped.put("id", String.valueOf(attributes.get("sub")));
        }
        return mapped;
    }

    private static String normalizeNaverGender(String raw) {
        if (raw == null) return null;
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "M" -> "MALE";
            case "F" -> "FEMALE";
            case "U" -> "UNDISCLOSED";
            default -> null;
        };
    }

    private static String combineBirthDate(String birthyear, String birthdayMd) {
        if (birthyear == null || birthdayMd == null) return null;

        String[] parts = birthdayMd.split("-");
        if (parts.length != 2) return null;

        String mm = parts[0];
        String dd = parts[1];
        if (mm.length() == 1) mm = "0" + mm;
        if (dd.length() == 1) dd = "0" + dd;

        return birthyear + "-" + mm + "-" + dd;
    }

    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
