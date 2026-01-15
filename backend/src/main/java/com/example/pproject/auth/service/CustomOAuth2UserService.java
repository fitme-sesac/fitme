package com.example.pproject.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * OAuth2 사용자 정보를 로드한다.
 *
 * - 신규 소셜 사용자 여부 판단/추가 가입 플로우로의 분기는 SuccessHandler에서 처리한다.
 * - 여기서 예외를 던지면 SuccessHandler까지 도달하지 못하므로(=신규 가입 페이지로 못 보냄)
 *   무조건 OAuth2User를 반환하도록 한다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        return new DefaultOAuth2UserService().loadUser(request);
    }
}