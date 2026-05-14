package com.benatti.auth.auth;

import com.benatti.auth.autoconfigure.AuthProperties;
import com.benatti.auth.dto.AuthResponse;
import com.benatti.auth.dto.UserDTO;
import com.benatti.auth.event.LoginFailureEvent;
import com.benatti.auth.event.LoginSuccessEvent;
import com.benatti.auth.event.LogoutEvent;
import com.benatti.auth.event.TokenRefreshEvent;
import com.benatti.auth.exception.InvalidCredentialsException;
import com.benatti.auth.exception.RefreshTokenExpiredException;
import com.benatti.auth.jwt.JwtException;
import com.benatti.auth.jwt.JwtPayload;
import com.benatti.auth.jwt.JwtTokenStore;
import com.benatti.auth.storage.RefreshToken;
import com.benatti.auth.storage.RefreshTokenRepository;
import com.benatti.auth.user.AuthUserDetails;
import com.benatti.auth.user.UserDetailsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultAuthService")
class DefaultAuthServiceTest {

    @Mock AuthenticationManager   authenticationManager;
    @Mock UserDetailsProvider     userDetailsProvider;
    @Mock JwtTokenStore           jwtTokenStore;
    @Mock RefreshTokenRepository  refreshTokenRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    private DefaultAuthService service;
    private AuthProperties     properties;

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        properties.setAccessTokenExpirationMinutes(60);
        properties.setRefreshTokenExpirationDays(30);
        service = new DefaultAuthService(
                authenticationManager, userDetailsProvider,
                jwtTokenStore, refreshTokenRepository, eventPublisher, properties);
    }

    // ── Stubs ──────────────────────────────────────────────────────────────

    private AuthUserDetails stubUser(String id, String username) {
        AuthUserDetails user = mock(AuthUserDetails.class);
        when(user.getUserId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        when(user.getEmail()).thenReturn(username + "@test.com");
        when(user.getPermissions()).thenReturn(List.of("READ:USERS"));
        when(user.getLastLogin()).thenReturn(Instant.now());
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(user).getAuthorities();
        return user;
    }

    // ── login ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("success — returns tokens and publishes LoginSuccessEvent")
        void success() {
            AuthUserDetails user = stubUser("uid-1", "alice");
            when(userDetailsProvider.loadUserByUsername("alice")).thenReturn(user);
            when(jwtTokenStore.generateAccessToken(user)).thenReturn("access-token");
            when(jwtTokenStore.generateRefreshToken(user)).thenReturn("refresh-token");

            AuthResponse response = service.login("alice", "secret");

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser().getUsername()).isEqualTo("alice");
            assertThat(response.getExpiresIn()).isEqualTo(3600);

            verify(refreshTokenRepository).save(any(RefreshToken.class));
            ArgumentCaptor<LoginSuccessEvent> cap =
                    ArgumentCaptor.forClass(LoginSuccessEvent.class);
            verify(eventPublisher).publishEvent(cap.capture());
            assertThat(cap.getValue().getUserId()).isEqualTo("uid-1");
        }

        @Test
        @DisplayName("bad credentials — throws InvalidCredentialsException and publishes LoginFailureEvent")
        void badCredentials() {
            doThrow(new BadCredentialsException("bad"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> service.login("alice", "wrong"))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(eventPublisher).publishEvent(any(LoginFailureEvent.class));
            verifyNoInteractions(jwtTokenStore, refreshTokenRepository);
        }
    }

    // ── refreshToken ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("success — returns new access token, publishes TokenRefreshEvent")
        void success() {
            JwtPayload payload = JwtPayload.builder()
                    .subject("uid-2").type("refresh").build();
            when(jwtTokenStore.validateToken("rt-123")).thenReturn(payload);

            com.benatti.auth.storage.RefreshToken storedRt =
                    com.benatti.auth.storage.RefreshToken.builder()
                            .token("rt-123").userId("uid-2").build();
            when(refreshTokenRepository.findByTokenAndValid("rt-123"))
                    .thenReturn(Optional.of(storedRt));

            AuthUserDetails user = stubUser("uid-2", "bob");
            when(userDetailsProvider.loadUserById("uid-2")).thenReturn(user);
            when(jwtTokenStore.generateAccessToken(user)).thenReturn("new-access");

            AuthResponse response = service.refreshToken("rt-123");

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("rt-123"); // unchanged
            verify(eventPublisher).publishEvent(any(TokenRefreshEvent.class));
        }

        @Test
        @DisplayName("invalid JWT — throws RefreshTokenExpiredException")
        void invalidJwt() {
            when(jwtTokenStore.validateToken("bad-rt"))
                    .thenThrow(new JwtException("expired", JwtException.ErrorType.EXPIRED));

            assertThatThrownBy(() -> service.refreshToken("bad-rt"))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("revoked token — throws RefreshTokenExpiredException")
        void revokedToken() {
            JwtPayload payload = JwtPayload.builder()
                    .subject("uid-2").type("refresh").build();
            when(jwtTokenStore.validateToken("rt-revoked")).thenReturn(payload);
            when(refreshTokenRepository.findByTokenAndValid("rt-revoked"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.refreshToken("rt-revoked"))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("wrong token type — throws RefreshTokenExpiredException")
        void wrongTokenType() {
            JwtPayload payload = JwtPayload.builder()
                    .subject("uid-2").type("access").build(); // access, not refresh
            when(jwtTokenStore.validateToken("access-as-refresh")).thenReturn(payload);

            assertThatThrownBy(() -> service.refreshToken("access-as-refresh"))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }
    }

    // ── logout ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("invalidates token and publishes LogoutEvent")
        void invalidatesAndPublishes() {
            service.logout("uid-1", "rt-xyz");

            verify(refreshTokenRepository).invalidate("rt-xyz");
            ArgumentCaptor<LogoutEvent> cap = ArgumentCaptor.forClass(LogoutEvent.class);
            verify(eventPublisher).publishEvent(cap.capture());
            assertThat(cap.getValue().getUserId()).isEqualTo("uid-1");
        }
    }

    // ── validateToken ──────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken() delegates to JwtTokenStore")
    void validateTokenDelegates() {
        when(jwtTokenStore.isValidToken("tok")).thenReturn(true);
        assertThat(service.validateToken("tok")).isTrue();

        when(jwtTokenStore.isValidToken("bad")).thenReturn(false);
        assertThat(service.validateToken("bad")).isFalse();
    }
}
