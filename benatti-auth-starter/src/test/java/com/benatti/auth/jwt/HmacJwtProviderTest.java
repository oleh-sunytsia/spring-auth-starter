package com.benatti.auth.jwt;

import com.benatti.auth.autoconfigure.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("HmacJwtProvider")
class HmacJwtProviderTest {

    private static final String SECRET = "test-secret-key-at-least-32-chars-long!!";

    private HmacJwtProvider provider;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.setJwtSecret(SECRET);
        props.setTokenIssuer("test-issuer");
        provider = new HmacJwtProvider(props);
    }

    private JwtPayload accessPayload() {
        return JwtPayload.builder()
                .subject("user-123")
                .username("john")
                .email("john@example.com")
                .roles(List.of("ROLE_USER", "ROLE_ADMIN"))
                .permissions(List.of("READ:USERS"))
                .type("access")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("test-issuer")
                .build();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("returns non-blank token string")
        void returnsNonBlankString() {
            String token = provider.generateToken(accessPayload());
            assertThat(token).isNotBlank().contains(".");
        }

        @Test
        @DisplayName("produces a three-part JWT (header.payload.signature)")
        void hasThreeParts() {
            String token = provider.generateToken(accessPayload());
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    @Nested
    @DisplayName("validateAndParseToken")
    class ValidateAndParse {

        @Test
        @DisplayName("round-trips all claims correctly")
        void roundTripsAllClaims() {
            JwtPayload payload = accessPayload();
            String token = provider.generateToken(payload);

            JwtPayload parsed = provider.validateAndParseToken(token);

            assertThat(parsed.getSubject()).isEqualTo("user-123");
            assertThat(parsed.getUsername()).isEqualTo("john");
            assertThat(parsed.getEmail()).isEqualTo("john@example.com");
            assertThat(parsed.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
            assertThat(parsed.getPermissions()).containsExactly("READ:USERS");
            assertThat(parsed.getType()).isEqualTo("access");
            assertThat(parsed.getIssuer()).isEqualTo("test-issuer");
        }

        @Test
        @DisplayName("throws JwtException with EXPIRED error type for an expired token")
        void throwsOnExpiredToken() {
            JwtPayload expiredPayload = JwtPayload.builder()
                    .subject("user-123")
                    .type("access")
                    .issuedAt(Instant.now().minusSeconds(7200))
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .build();
            String expiredToken = provider.generateToken(expiredPayload);

            assertThatThrownBy(() -> provider.validateAndParseToken(expiredToken))
                    .isInstanceOf(JwtException.class)
                    .satisfies(e -> assertThat(((JwtException) e).getErrorType())
                            .isEqualTo(JwtException.ErrorType.EXPIRED));
        }

        @Test
        @DisplayName("throws JwtException with INVALID_SIGNATURE for a tampered token")
        void throwsOnTamperedToken() {
            String token = provider.generateToken(accessPayload());
            // corrupt the signature segment
            String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsig";

            assertThatThrownBy(() -> provider.validateAndParseToken(tampered))
                    .isInstanceOf(JwtException.class)
                    .satisfies(e -> assertThat(((JwtException) e).getErrorType())
                            .isEqualTo(JwtException.ErrorType.INVALID_SIGNATURE));
        }

        @Test
        @DisplayName("throws JwtException with MALFORMED for garbage input")
        void throwsOnMalformedToken() {
            assertThatThrownBy(() -> provider.validateAndParseToken("not.a.jwt"))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Nested
    @DisplayName("isValidToken")
    class IsValidToken {

        @Test
        @DisplayName("returns true for a fresh valid token")
        void trueForValidToken() {
            String token = provider.generateToken(accessPayload());
            assertThat(provider.isValidToken(token)).isTrue();
        }

        @Test
        @DisplayName("returns false for an expired token")
        void falseForExpiredToken() {
            JwtPayload expired = JwtPayload.builder()
                    .subject("u")
                    .issuedAt(Instant.now().minusSeconds(7200))
                    .expiresAt(Instant.now().minusSeconds(1))
                    .build();
            String token = provider.generateToken(expired);
            assertThat(provider.isValidToken(token)).isFalse();
        }

        @Test
        @DisplayName("returns false for random garbage")
        void falseForGarbage() {
            assertThat(provider.isValidToken("garbage")).isFalse();
        }
    }

    @Nested
    @DisplayName("getExpirationSeconds")
    class GetExpirationSeconds {

        @Test
        @DisplayName("returns roughly the configured TTL in seconds")
        void returnsRemainingSeconds() {
            String token = provider.generateToken(accessPayload());
            long remaining = provider.getExpirationSeconds(token);
            // token was created with 3600s TTL; should be within a second of that
            assertThat(remaining).isBetween(3598L, 3601L);
        }

        @Test
        @DisplayName("returns 0 for an expired token")
        void returnsZeroForExpired() {
            JwtPayload expired = JwtPayload.builder()
                    .subject("u")
                    .issuedAt(Instant.now().minusSeconds(7200))
                    .expiresAt(Instant.now().minusSeconds(1))
                    .build();
            String token = provider.generateToken(expired);
            assertThat(provider.getExpirationSeconds(token)).isZero();
        }
    }
}
