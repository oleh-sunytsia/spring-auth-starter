package com.benatti.auth.jwt;

import com.benatti.auth.autoconfigure.AuthProperties;
import com.benatti.auth.user.AuthUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenStore")
class JwtTokenStoreTest {

    private JwtTokenStore tokenStore;
    private AuthProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        properties.setJwtSecret("test-secret-key-at-least-32-chars-long!!");
        properties.setTokenIssuer("test");
        properties.setAccessTokenExpirationMinutes(60);
        properties.setRefreshTokenExpirationDays(30);

        JwtProvider provider = new HmacJwtProvider(properties);
        tokenStore = new JwtTokenStore(provider, properties);
    }

    private AuthUserDetails fakeUser() {
        return new AuthUserDetails() {
            public String getUserId()           { return "uid-1"; }
            public String getEmail()            { return "user@test.com"; }
            public List<String> getPermissions(){ return List.of("READ:USERS"); }
            public Instant getLastLogin()       { return Instant.now(); }
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }
            public String getPassword()  { return "hashed"; }
            public String getUsername()  { return "user1"; }
            public boolean isEnabled()              { return true; }
            public boolean isAccountNonExpired()    { return true; }
            public boolean isAccountNonLocked()     { return true; }
            public boolean isCredentialsNonExpired(){ return true; }
        };
    }

    @Test
    @DisplayName("generateAccessToken — token is valid and type=access")
    void generateAccessToken_isValidAndTypeAccess() {
        String token = tokenStore.generateAccessToken(fakeUser());

        assertThat(token).isNotBlank();
        JwtPayload payload = tokenStore.validateToken(token);
        assertThat(payload.getSubject()).isEqualTo("uid-1");
        assertThat(payload.getType()).isEqualTo("access");
        assertThat(payload.getRoles()).contains("ROLE_USER");
    }

    @Test
    @DisplayName("generateRefreshToken — token is valid and type=refresh")
    void generateRefreshToken_isValidAndTypeRefresh() {
        String token = tokenStore.generateRefreshToken(fakeUser());

        assertThat(token).isNotBlank();
        JwtPayload payload = tokenStore.validateToken(token);
        assertThat(payload.getSubject()).isEqualTo("uid-1");
        assertThat(payload.getType()).isEqualTo("refresh");
        // refresh token should not carry roles/permissions
        assertThat(payload.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("isValidToken — true for fresh, false for garbage")
    void isValidToken() {
        String token = tokenStore.generateAccessToken(fakeUser());
        assertThat(tokenStore.isValidToken(token)).isTrue();
        assertThat(tokenStore.isValidToken("bad-token")).isFalse();
    }

    @Test
    @DisplayName("getExpirationSeconds — within expected window")
    void getExpirationSeconds() {
        String token = tokenStore.generateAccessToken(fakeUser());
        long seconds = tokenStore.getExpirationSeconds(token);
        // 60 minutes = 3600 seconds; allow 2-second delta
        assertThat(seconds).isBetween(3598L, 3602L);
    }
}
