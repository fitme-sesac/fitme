package com.example.pproject.Config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * JWT로부터 복원되는 최소 사용자 정보(무상태).
 * - userid: 로그인 식별자(authentication.getName())
 * - displayName: 화면 표시용 이름
 */
public class JwtUserPrincipal implements UserDetails {

    private final String userid;
    private final String displayName;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtUserPrincipal(String userid, String displayName, Collection<? extends GrantedAuthority> authorities) {
        this.userid = userid;
        this.displayName = displayName;
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    public String getUserid() {
        return userid;
    }

    public String getDisplayName() {
        return displayName;
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
