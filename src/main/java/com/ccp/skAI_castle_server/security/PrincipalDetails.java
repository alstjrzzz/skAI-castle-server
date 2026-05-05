package com.ccp.skAI_castle_server.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class PrincipalDetails implements UserDetails {

    private final String uuid;
    private final String role;

    private PrincipalDetails(String uuid, String role) {
        this.uuid = uuid;
        this.role = role;
    }

    public static PrincipalDetails of(String uuid, String role) {
        return new PrincipalDetails(uuid, role);
    }

    public String getUuid() { return uuid; }
    public String getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getUsername() { return uuid; }
    @Override
    public String getPassword() { return null; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}