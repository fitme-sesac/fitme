package com.example.pproject.auth.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthStatusController {

    @GetMapping("/api/auth/status")
    public Map<String, Object> status() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        String name = null;
        if (authenticated) {
            Object principal = auth.getPrincipal();

            // 1) JWT 로그인: displayName(claim) 우선
            if (principal instanceof JwtUserPrincipal jwtPrincipal) {
                name = jwtPrincipal.getDisplayName();
            }

            // 2) OAuth2 로그인(세션 기반): OAuth2User의 name/email 속성 활용
            if ((name == null || name.isBlank()) && principal instanceof OAuth2User oauth2User) {
                name = oauth2User.getAttribute("name");
                if (name == null || name.isBlank()) {
                    name = oauth2User.getName();
                }
            }

            // 3) 기타(UserDetails 등): 최소한 auth.getName()은 내려준다
            if (name == null || name.isBlank()) {
                name = auth.getName();
            }
        }
        return Map.of(
                "authenticated", authenticated,
                "name", name
        );
    }
}
