package com.example.pproject.Config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret; // 최소 32자 이상 권장

    @Getter
    @Value("${jwt.access-token-validity-seconds:1800}")
    private long accessTokenValiditySeconds;

    private Key key;

    @PostConstruct
    public void init() {
        // 문자열 길이가 32자 이상이 되도록 application.properties에 길게 넣어줘
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // JWT 생성
    public String createAccessToken(Authentication authentication) {
        return createAccessToken(authentication, null, null);
    }

    /**
     * access token 생성
     * - 화면 헤더 등에 표시할 이름(displayName)과 이메일(email)을 함께 담아 무상태에서도 표시 가능하게 한다.
     */
    public String createAccessToken(Authentication authentication, String displayName, String email) {
        String username = authentication.getName();
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValiditySeconds * 1000);

        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("auth", authorities)
                .setIssuedAt(now)
                .setExpiration(expiry);

        if (displayName != null && !displayName.isBlank()) {
            builder.claim("displayName", displayName);
        }
        if (email != null && !email.isBlank()) {
            builder.claim("email", email);
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    // 토큰에서 Authentication 뽑기
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        String username = claims.getSubject();
        String authString = claims.get("auth", String.class);

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (authString != null && !authString.isBlank()) {
            authorities = Arrays.stream(authString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        String displayName = claims.get("displayName", String.class);
        String email = claims.get("email", String.class);

        // 무상태 화면 표시를 위해 최소 정보만 가진 principal 사용 (email 포함)
        JwtUserPrincipal principal = new JwtUserPrincipal(username, displayName, email, authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * 임시 플로우 토큰 생성 (예: 소셜 최초 가입 화면 전달용)
     * 
     * @param flowType        플로우 종류 (예: OAUTH2_REGISTER)
     * @param claims          추가로 담을 데이터 (예: name, email)
     * @param validitySeconds 만료(초)
     */
    public String createFlowToken(String flowType, Map<String, Object> claims, long validitySeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validitySeconds * 1000);

        JwtBuilder builder = Jwts.builder()
                .setSubject("FLOW")
                .claim("flowType", flowType)
                .setIssuedAt(now)
                .setExpiration(expiry);

        if (claims != null && !claims.isEmpty()) {
            builder.claim("claims", claims);
        }
        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    /**
     * 토큰에서 Claims 추출 (검증/파싱 실패 시 예외 발생)
     */
    public Claims getClaims(String token) {
        return parseClaims(token);
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.", e);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.", e);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어 있습니다.", e);
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
