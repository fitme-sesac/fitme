package com.example.pproject.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;

/**
 * OAuth2AuthorizedClient를 세션에 저장하지 않도록 하는 No-Op 구현.
 * (로그인 콜백 처리에 필요한 정보는 Spring 내부에서 처리되며, 우리는 JWT만 발급/사용한다.)
 */
@Component
public class NoOpOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {
    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal,
                                                                     HttpServletRequest request) {
        return null;
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal,
                                    HttpServletRequest request, HttpServletResponse response) {
        // no-op
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, Authentication principal,
                                       HttpServletRequest request, HttpServletResponse response) {
        // no-op
    }
}
