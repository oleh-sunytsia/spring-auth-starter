package com.benatti.auth.controller;

import com.benatti.auth.auth.AuthService;
import com.benatti.auth.autoconfigure.AuthProperties;
import com.benatti.auth.dto.AuthResponse;
import com.benatti.auth.dto.LoginRequest;
import com.benatti.auth.dto.RefreshTokenRequest;
import com.benatti.auth.dto.UserDTO;
import com.benatti.auth.dto.ValidateTokenResponse;
import com.benatti.auth.exception.InvalidCredentialsException;
import com.benatti.auth.user.AuthUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock AuthService authService;

    private AuthController controller;
    private AuthProperties  properties;

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        controller = new AuthController(authService, properties);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private AuthResponse mockAuthResponse(String access, String refresh) {
        UserDTO user = UserDTO.builder()
                .id("uid-1").username("alice").email("alice@test.com")
                .roles(List.of("ROLE_USER")).permissions(List.of()).build();
        return AuthResponse.builder()
                .accessToken(access).refreshToken(refresh).expiresIn(3600).user(user)
                .build();
    }

    private AuthUserDetails mockUserDetails() {
        AuthUserDetails ud = mock(AuthUserDetails.class);
        when(ud.getUserId()).thenReturn("uid-1");
        lenient().when(ud.getUsername()).thenReturn("alice");
        lenient().when(ud.getEmail()).thenReturn("alice@test.com");
        lenient().when(ud.getPermissions()).thenReturn(List.of());
        lenient().when(ud.getLastLogin()).thenReturn(Instant.now());
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(ud).getAuthorities();
        return ud;
    }

    // ── login ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("HTTP 200 with AuthResponse on success")
        void returnsOkOnSuccess() {
            AuthResponse resp = mockAuthResponse("acc", "ref");
            when(authService.login("alice", "pass")).thenReturn(resp);

            LoginRequest req = LoginRequest.builder().username("alice").password("pass").build();
            ResponseEntity<AuthResponse> result = controller.login(req);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getAccessToken()).isEqualTo("acc");
        }

        @Test
        @DisplayName("propagates InvalidCredentialsException from AuthService")
        void propagatesException() {
            when(authService.login(any(), any()))
                    .thenThrow(new InvalidCredentialsException("bad credentials"));

            LoginRequest req = LoginRequest.builder().username("x").password("y").build();
            assertThatThrownBy(() -> controller.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    // ── refresh ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("HTTP 200 with new access token")
        void returnsNewToken() {
            when(authService.refreshToken("rt-old"))
                    .thenReturn(mockAuthResponse("new-acc", "rt-old"));

            RefreshTokenRequest req =
                    RefreshTokenRequest.builder().refreshToken("rt-old").build();
            ResponseEntity<AuthResponse> result = controller.refresh(req);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getAccessToken()).isEqualTo("new-acc");
        }
    }

    // ── logout ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("HTTP 204 No Content and invalidates token")
        void returns204() {
            AuthUserDetails ud = mockUserDetails();
            when(authService.getUserDetailsFromToken("rt-xyz")).thenReturn(ud);

            RefreshTokenRequest req =
                    RefreshTokenRequest.builder().refreshToken("rt-xyz").build();
            ResponseEntity<Void> result = controller.logout(req);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService).logout("uid-1", "rt-xyz");
        }

        @Test
        @DisplayName("still returns 204 when token is unresolvable (best-effort)")
        void returns204EvenOnBadToken() {
            when(authService.getUserDetailsFromToken(any()))
                    .thenThrow(new com.benatti.auth.exception.AuthException("invalid"));

            RefreshTokenRequest req =
                    RefreshTokenRequest.builder().refreshToken("garbage").build();
            ResponseEntity<Void> result = controller.logout(req);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService).logout(null, "garbage");
        }
    }

    // ── validate ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        @DisplayName("returns valid=true with user details for a good token")
        void validToken() {
            AuthUserDetails ud = mockUserDetails();
            when(authService.validateToken("good-token")).thenReturn(true);
            when(authService.getUserDetailsFromToken("good-token")).thenReturn(ud);

            ResponseEntity<ValidateTokenResponse> result =
                    controller.validate("Bearer good-token", null);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isValid()).isTrue();
            assertThat(result.getBody().getUserId()).isEqualTo("uid-1");
        }

        @Test
        @DisplayName("returns valid=false for an invalid token")
        void invalidToken() {
            when(authService.validateToken("bad-token")).thenReturn(false);

            ResponseEntity<ValidateTokenResponse> result =
                    controller.validate("Bearer bad-token", null);

            assertThat(result.getBody().isValid()).isFalse();
        }

        @Test
        @DisplayName("accepts token from query param when header is absent")
        void acceptsQueryParam() {
            AuthUserDetails ud = mockUserDetails();
            when(authService.validateToken("param-token")).thenReturn(true);
            when(authService.getUserDetailsFromToken("param-token")).thenReturn(ud);

            ResponseEntity<ValidateTokenResponse> result =
                    controller.validate(null, "param-token");

            assertThat(result.getBody().isValid()).isTrue();
        }
    }
}
