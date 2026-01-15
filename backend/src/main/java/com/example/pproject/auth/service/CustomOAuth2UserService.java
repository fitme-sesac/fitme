package com.example.pproject.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String regId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oauth2User.getAttributes();

        // keep all original attributes (so nameAttributeKey like "sub"(google) / "id"(kakao) still exists)
        Map<String, Object> mapped = new LinkedHashMap<>(attrs);

        try {
            if ("kakao".equalsIgnoreCase(regId)) {
                String email = null;
                String name = null;

                Object kakaoAccountObj = attrs.get("kakao_account");
                if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
                    Object emailObj = kakaoAccount.get("email");
                    if (emailObj != null) email = String.valueOf(emailObj);

                    Object profileObj = kakaoAccount.get("profile");
                    if (profileObj instanceof Map<?, ?> profile) {
                        Object nicknameObj = profile.get("nickname");
                        if (nicknameObj != null) name = String.valueOf(nicknameObj);
                    }
                }

                // fallback (some responses expose nickname under "properties")
                if (name == null) {
                    Object propsObj = attrs.get("properties");
                    if (propsObj instanceof Map<?, ?> props) {
                        Object nickObj = props.get("nickname");
                        if (nickObj != null) name = String.valueOf(nickObj);
                    }
                }

                if (email != null && !email.isBlank()) mapped.put("email", email);
                if (name != null && !name.isBlank()) mapped.put("name", name);

            } else {
                // google/others: typically already provides "email" and "name"
                Object emailObj = attrs.get("email");
                if (emailObj != null) mapped.put("email", String.valueOf(emailObj));

                Object nameObj = attrs.get("name");
                if (nameObj != null) mapped.put("name", String.valueOf(nameObj));
            }
        } catch (Exception e) {
            log.warn("OAuth2 attribute mapping failed. regId={}", regId, e);
        }

        String nameKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        if (nameKey == null || nameKey.isBlank() || !mapped.containsKey(nameKey)) {
            // defensive fallback
            if (mapped.containsKey("id")) nameKey = "id";
            else if (mapped.containsKey("sub")) nameKey = "sub";
            else if (mapped.containsKey("email")) nameKey = "email";
            else if (mapped.containsKey("name")) nameKey = "name";
            else {
                mapped.put("id", "UNKNOWN");
                nameKey = "id";
            }
        }

        return new DefaultOAuth2User(oauth2User.getAuthorities(), mapped, nameKey);
    }
}
