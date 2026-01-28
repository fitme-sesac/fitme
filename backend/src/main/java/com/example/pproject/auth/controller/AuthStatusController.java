package com.example.pproject.auth.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthStatusController {

    @GetMapping("/api/auth/status")
    public Map<String, Object> status(Authentication auth) {

        boolean jwtAuthenticated =
                auth != null
                        && auth.isAuthenticated()
                        && !(auth instanceof AnonymousAuthenticationToken)
                        && (auth.getPrincipal() instanceof JwtUserPrincipal);

        if (!jwtAuthenticated) {
            // Map.of는 null 금지 -> 빈 문자열 사용
            return Map.of("authenticated", false, "name", "", "role", "");
        }

        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        String name = (p.getDisplayName() == null || p.getDisplayName().isBlank())
                ? p.getUserid()
                : p.getDisplayName();

        // role 추출 (ROLE_ 접두사 제거)
        String role = p.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("");

        return Map.of("authenticated", true, "name", name, "role", role);
    }
}