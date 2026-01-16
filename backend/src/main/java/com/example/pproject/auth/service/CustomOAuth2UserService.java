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

    /**
     * Load and normalize an OAuth2 user into a provider-agnostic attribute map.
     *
     * <p>This method delegates to the default loader to obtain the original OAuth2User,
     * maps provider-specific attributes (NAVER, KAKAO, GOOGLE) into a standardized
     * attribute set, determines the appropriate name attribute key, and returns a
     * DefaultOAuth2User constructed from the original authorities and the normalized attributes.
     *
     * @param userRequest the OAuth2 user request containing client registration and access token
     * @return an OAuth2User whose attributes are normalized for known providers and whose name attribute key
     *         is selected from the mapped attributes or the client configuration
     * @throws OAuth2AuthenticationException if the provider response cannot be parsed or is invalid (for example, an invalid NAVER response)
     */
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

    /**
     * Selects which attribute key should serve as the user's name/identifier.
     *
     * Prefers the provided `preferred` key when it exists in `attributes`; otherwise
     * chooses `"id"`, then `"sub"`, then `"email"`, then the first available key,
     * and `"id"` if the map is empty.
     *
     * @param attributes the map of user attributes to choose from
     * @param preferred  an optional preferred attribute name to use if present in `attributes`
     * @return the chosen attribute key to use as the user's name/identifier
     */
    private static String chooseNameKey(Map<String, Object> attributes, String preferred) {
        if (preferred != null && attributes.containsKey(preferred)) return preferred;
        if (attributes.containsKey("id")) return "id";
        if (attributes.containsKey("sub")) return "sub";
        if (attributes.containsKey("email")) return "email";
        return attributes.keySet().stream().findFirst().orElse("id");
    }

    /**
     * Map raw NAVER OAuth2 userinfo into a standardized attribute map.
     *
     * @param attributes the original OAuth2 attributes returned by NAVER; must contain a "response" map
     * @return a LinkedHashMap containing the standardized keys:
     *         - "id" (String)
     *         - "provider" (String) set to "NAVER"
     *         - "email" (String or null)
     *         - "name" (String or null)
     *         - "gender" ("MALE", "FEMALE", "UNDISCLOSED", or null)
     *         - "birthday" (String in "YYYY-MM-DD" or null)
     *         - "phone" (digits-only String or null)
     *         - "raw" (the original NAVER response map)
     * @throws OAuth2AuthenticationException if the NAVER response map is missing or does not contain a valid "id"
     */
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

    /**
     * Maps Kakao OAuth2 user attributes into a standardized attribute map for the application.
     *
     * @param attributes the raw attribute map returned by Kakao's OAuth2 provider
     * @return a LinkedHashMap containing standardized keys:
     *         "provider" ("KAKAO"), "id" (string or null), "email" (string or null),
     *         "name" (always null), and "raw" (the original attributes)
     */
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

    /**
     * Map Google OAuth2 user attributes into the application's standardized attribute shape.
     *
     * <p>Produces a LinkedHashMap preserving the original Google attributes, ensures a
     * "provider" key set to "GOOGLE", and, if present, copies the Google's "sub" value
     * into the standardized "id" key as a string.
     *
     * @param attributes the raw attributes returned by Google's OAuth2 provider
     * @return a mapped attributes map containing the original values plus a "provider"
     *         entry and an "id" entry when the source "sub" value is present
     */
    private static Map<String, Object> mapGoogle(Map<String, Object> attributes) {
        Map<String, Object> mapped = new LinkedHashMap<>(attributes);
        mapped.put("provider", "GOOGLE");
        if (attributes.get("sub") != null) {
            mapped.put("id", String.valueOf(attributes.get("sub")));
        }
        return mapped;
    }

    /**
     * Normalize a raw Naver gender code to a standardized gender label.
     *
     * @param raw the raw gender value returned by Naver (e.g. "M", "F", "U"); may be null and is compared case-insensitively
     * @return `MALE`, `FEMALE`, or `UNDISCLOSED` when the input matches a known code; `null` if the input is null or unrecognized
     */
    private static String normalizeNaverGender(String raw) {
        if (raw == null) return null;
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "M" -> "MALE";
            case "F" -> "FEMALE";
            case "U" -> "UNDISCLOSED";
            default -> null;
        };
    }

    /**
     * Combine a four-digit year and a month-day fragment into an ISO date string.
     *
     * <p>Expects {@code birthyear} to contain the year (e.g., "1985") and {@code birthdayMd}
     * to contain month and day separated by a hyphen in "M-D" or "MM-DD" form (e.g., "7-9" or "07-09").
     *
     * @param birthyear  the year component (expected "YYYY")
     * @param birthdayMd the month-day component ("M-D" or "MM-DD")
     * @return the combined date as "YYYY-MM-DD" with zero-padded month and day, or {@code null}
     *         if either input is {@code null} or {@code birthdayMd} is not in the expected "M-D" format
     */
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

    /**
     * Normalize a phone number by removing all non-digit characters.
     *
     * @param raw the input phone string that may contain spaces, punctuation, or other characters
     * @return the phone number containing only digits, or `null` if the input is `null` or contains no digits
     */
    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    /**
     * Convert an object to its String representation or return null.
     *
     * @param v the object to convert; may be null
     * @return `null` if {@code v} is null, otherwise {@code v}'s string representation
     */
    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}