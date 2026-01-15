package com.example.pproject.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Inspects the request for an ACCESS_TOKEN cookie, validates the JWT, and establishes a corresponding Authentication in the SecurityContext when appropriate.
     *
     * If a valid token is present, the filter sets the SecurityContext authentication only when there is no current authentication, the current authentication is an AnonymousAuthenticationToken, or the current principal is not an instance of JwtUserPrincipal. Processing always continues by delegating to the filter chain.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication current = SecurityContextHolder.getContext().getAuthentication();

            boolean shouldOverride =
                    current == null
                            || current instanceof AnonymousAuthenticationToken
                            || !(current.getPrincipal() instanceof JwtUserPrincipal);

            if (shouldOverride) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT access token from the request cookies.
     *
     * @param request the HTTP request whose cookies will be inspected
     * @return the value of the "ACCESS_TOKEN" cookie if present and non-empty, or `null` if not found
     */
    private String resolveToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(cookie.getName()) &&
                        StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}