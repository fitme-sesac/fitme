package com.example.pproject.Config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * JWT로부터 복원되는 최소 사용자 정보(무상태).
 * - userid: 로그인 식별자(authentication.getName())
 * - displayName: 화면 표시용 이름
 * - email: 사용자 이메일 (소셜 로그인 사용자 조회용)
 */
public class JwtUserPrincipal implements UserDetails {

    // [추가] DB PK (기존 생성자 호환을 위해 final 제외)
    private Long id;

    private final String userid;
    private final String displayName;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    // [기존 생성자 1] - 변경 없음
    public JwtUserPrincipal(String userid, String displayName, Collection<? extends GrantedAuthority> authorities) {
        this(userid, displayName, null, authorities);
    }

    // [기존 생성자 2] - 변경 없음
    public JwtUserPrincipal(String userid, String displayName, String email,
                            Collection<? extends GrantedAuthority> authorities) {
        this.userid = userid;
        this.displayName = displayName;
        this.email = email;
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    // [추가] ID를 포함하는 새로운 생성자 (기존 생성자 재사용)
    public JwtUserPrincipal(Long id, String userid, String displayName, String email,
                            Collection<? extends GrantedAuthority> authorities) {
        this(userid, displayName, email, authorities); // 기존 로직 태움
        this.id = id; // ID만 추가 세팅
    }

    // [추가] Getter
    public Long getId() {
        return id;
    }

    public String getUserid() {
        return userid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return userid;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}