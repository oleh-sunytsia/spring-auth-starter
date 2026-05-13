package com.benatti.auth.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of AuthUserDetails.
 * Developers can use this directly or create their own implementation.
 */
public class DefaultAuthUserDetails implements AuthUserDetails {

    private final String              userId;
    private final String              username;
    private final String              password;
    private final String              email;
    private final List<String>        roles;
    private final List<String>        permissions;
    private final Instant             lastLogin;
    private final boolean             enabled;
    private final boolean             accountNonExpired;
    private final boolean             accountNonLocked;
    private final boolean             credentialsNonExpired;

    private DefaultAuthUserDetails(Builder b) {
        this.userId                = b.userId;
        this.username              = b.username;
        this.password              = b.password;
        this.email                 = b.email;
        this.roles                 = b.roles != null ? b.roles : List.of();
        this.permissions           = b.permissions != null ? b.permissions : List.of();
        this.lastLogin             = b.lastLogin;
        this.enabled               = b.enabled;
        this.accountNonExpired     = b.accountNonExpired;
        this.accountNonLocked      = b.accountNonLocked;
        this.credentialsNonExpired = b.credentialsNonExpired;
    }

    // ── AuthUserDetails ────────────────────────────────────────────────────

    @Override
    public String getUserId() { return userId; }

    @Override
    public String getEmail() { return email; }

    @Override
    public List<String> getPermissions() { return permissions; }

    @Override
    public Instant getLastLogin() { return lastLogin; }

    // ── UserDetails ────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(
                        role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean isAccountNonExpired() { return accountNonExpired; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return credentialsNonExpired; }

    // ── Builder ────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String       userId;
        private String       username;
        private String       password;
        private String       email;
        private List<String> roles;
        private List<String> permissions;
        private Instant      lastLogin;
        private boolean      enabled               = true;
        private boolean      accountNonExpired     = true;
        private boolean      accountNonLocked      = true;
        private boolean      credentialsNonExpired = true;

        public Builder userId(String userId)                     { this.userId = userId; return this; }
        public Builder username(String username)                 { this.username = username; return this; }
        public Builder password(String password)                 { this.password = password; return this; }
        public Builder email(String email)                       { this.email = email; return this; }
        public Builder roles(List<String> roles)                 { this.roles = roles; return this; }
        public Builder permissions(List<String> permissions)     { this.permissions = permissions; return this; }
        public Builder lastLogin(Instant lastLogin)              { this.lastLogin = lastLogin; return this; }
        public Builder enabled(boolean enabled)                  { this.enabled = enabled; return this; }
        public Builder accountNonExpired(boolean v)              { this.accountNonExpired = v; return this; }
        public Builder accountNonLocked(boolean v)               { this.accountNonLocked = v; return this; }
        public Builder credentialsNonExpired(boolean v)          { this.credentialsNonExpired = v; return this; }

        public DefaultAuthUserDetails build() {
            if (userId   == null) throw new IllegalStateException("userId is required");
            if (username == null) throw new IllegalStateException("username is required");
            return new DefaultAuthUserDetails(this);
        }
    }
}
