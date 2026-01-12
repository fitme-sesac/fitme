package com.example.pproject.Config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * OAuth2AuthorizationRequest를 "서명된 JSON" 형태로 HttpOnly 쿠키에 저장.
 * - Java Serialization/Deserialization 제거
 * - HMAC-SHA256 서명으로 무결성 보장
 */
@Component
@RequiredArgsConstructor
public class SignedCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final ObjectMapper objectMapper;

    @Value("${oauth2.auth-request-cookie.name:OAUTH2_AUTH_REQUEST}")
    private String cookieName;

    @Value("${oauth2.auth-request-cookie.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${oauth2.auth-request-cookie.secret}")
    private String hmacSecret;

    private SecretKeySpec hmacKey;

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(hmacSecret) || hmacSecret.length() < 32) {
            throw new IllegalStateException("oauth2.auth-request-cookie.secret must be at least 32 chars.");
        }
        this.hmacKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookieValue(request, cookieName)
                .map(this::verifyAndDeserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        OAuth2AuthRequestCookie payload = OAuth2AuthRequestCookie.from(authorizationRequest, ttlSeconds);
        String value = signAndSerialize(payload);

        CookieUtils.addHttpOnlyCookie(request, response, cookieName, value, ttlSeconds, "Lax");
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest req = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return req;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, cookieName);
    }

    // ===== Serialization (JSON) + Signing =====

    private String signAndSerialize(OAuth2AuthRequestCookie payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            String bodyB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(json);
            String sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacSha256(bodyB64));
            return bodyB64 + "." + sigB64;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize OAuth2AuthRequestCookie", e);
        }
    }

    private OAuth2AuthorizationRequest verifyAndDeserialize(String cookieValue) {
        try {
            String[] parts = cookieValue.split("\\.");
            if (parts.length != 2) return null;

            String bodyB64 = parts[0];
            String sigB64 = parts[1];

            byte[] expectedSig = hmacSha256(bodyB64);
            byte[] givenSig = Base64.getUrlDecoder().decode(sigB64);

            if (!MessageDigest.isEqual(expectedSig, givenSig)) {
                return null;
            }

            byte[] json = Base64.getUrlDecoder().decode(bodyB64);
            OAuth2AuthRequestCookie payload =
                    objectMapper.readValue(json, new TypeReference<>() {});

            // 만료 체크(서버 기준)
            if (payload.expEpochSec == null || Instant.now().getEpochSecond() > payload.expEpochSec) {
                return null;
            }

            return payload.toAuthorizationRequest();

        } catch (Exception e) {
            return null;
        }
    }

    private byte[] hmacSha256(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(hmacKey);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    // ===== DTO =====
    public static class OAuth2AuthRequestCookie {
        public String authorizationUri;
        public String clientId;
        public String redirectUri;
        public Set<String> scopes;
        public String state;
        public Map<String, String> additionalParameters;
        public Map<String, String> attributes;
        public Long expEpochSec;

        static OAuth2AuthRequestCookie from(OAuth2AuthorizationRequest req, long ttlSeconds) {
            OAuth2AuthRequestCookie dto = new OAuth2AuthRequestCookie();
            dto.authorizationUri = req.getAuthorizationUri();
            dto.clientId = req.getClientId();
            dto.redirectUri = req.getRedirectUri();
            dto.scopes = (req.getScopes() == null) ? null : new LinkedHashSet<>(req.getScopes());
            dto.state = req.getState();
            dto.additionalParameters = toStringMap(req.getAdditionalParameters());
            dto.attributes = toStringMap(req.getAttributes());
            dto.expEpochSec = Instant.now().getEpochSecond() + ttlSeconds;
            return dto;
        }

        OAuth2AuthorizationRequest toAuthorizationRequest() {
            Map<String, Object> ap = new LinkedHashMap<>();
            if (additionalParameters != null) ap.putAll(additionalParameters);

            Map<String, Object> attrs = new LinkedHashMap<>();
            if (attributes != null) attrs.putAll(attributes);

            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes == null ? Collections.emptySet() : scopes)
                    .state(state)
                    .additionalParameters(ap)
                    .attributes(attrs)
                    .build();
        }

        private static Map<String, String> toStringMap(Map<String, Object> src) {
            if (src == null || src.isEmpty()) return null;

            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : src.entrySet()) {
                if (e.getValue() == null) continue;
                // OAuth2 AuthorizationRequest에 들어가는 값들은 대부분 String이어야 정상 동작(특히 PKCE code_verifier 포함)
                out.put(e.getKey(), String.valueOf(e.getValue()));
            }
            return out.isEmpty() ? null : out;
        }
    }
}
