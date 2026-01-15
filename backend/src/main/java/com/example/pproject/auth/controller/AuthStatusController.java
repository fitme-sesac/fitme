package com.example.pproject.auth.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthStatusController {

    /**
     * Provides the caller's authentication status and a display name for the authenticated user.
     *
     * <p>The returned map contains two entries:
     * - "authenticated": `true` if the request is authenticated with a JwtUserPrincipal, `false` otherwise.
     * - "name": an empty string when not authenticated; otherwise the principal's display name if present, or the principal's user id.
     *
     * @param auth the current Spring Security Authentication (may be null or represent an anonymous/principal-less state)
     * @return a map with keys "authenticated" and "name" describing the authentication state and a human-visible name
     */
    @GetMapping("/api/auth/status")
    public Map<String, Object> status(Authentication auth) {

        boolean jwtAuthenticated =
                auth != null
                        && auth.isAuthenticated()
                        && !(auth instanceof AnonymousAuthenticationToken)
                        && (auth.getPrincipal() instanceof JwtUserPrincipal);

        if (!jwtAuthenticated) {
            // Map.of는 null 금지 -> 빈 문자열 사용
            return Map.of("authenticated", false, "name", "");
        }

        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        String name = (p.getDisplayName() == null || p.getDisplayName().isBlank())
                ? p.getUserid()
                : p.getDisplayName();

        return Map.of("authenticated", true, "name", name);
    }
}