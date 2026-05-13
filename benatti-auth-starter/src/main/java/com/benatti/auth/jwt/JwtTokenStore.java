package com.benatti.auth.jwt;

import com.benatti.auth.autoconfigure.AuthProperties;
import com.benatti.auth.user.AuthUserDetails;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Helper that builds access / refresh tokens from AuthUserDetails,
 * delegating actual signing to JwtProvider.
 */
public class JwtTokenStore {

    private final JwtProvider   jwtProvider;
    private final AuthProperties properties;

    public JwtTokenStore(JwtProvider jwtProvider, AuthProperties properties) {
        this.jwtProvider = jwtProvider;
        this.properties  = properties;
    }

    /** Generate short-lived access token. */
    public String generateAccessToken(AuthUserDetails user) {
        JwtPayload payload = JwtPayload.builder()
                .subject(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList()))
                .permissions(user.getPermissions())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(
                        Duration.ofMinutes(properties.getAccessTokenExpirationMinutes())))
                .issuer(properties.getTokenIssuer())
                .type("access")
                .build();

        return jwtProvider.generateToken(payload);
    }

    /** Generate long-lived refresh token. */
    public String generateRefreshToken(AuthUserDetails user) {
        JwtPayload payload = JwtPayload.builder()
                .subject(user.getUserId())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(
                        Duration.ofDays(properties.getRefreshTokenExpirationDays())))
                .issuer(properties.getTokenIssuer())
                .type("refresh")
                .build();

        return jwtProvider.generateToken(payload);
    }

    /** Validate and extract claims from any token. */
    public JwtPayload validateToken(String token) throws JwtException {
        return jwtProvider.validateAndParseToken(token);
    }

    public boolean isValidToken(String token) {
        return jwtProvider.isValidToken(token);
    }

    public long getExpirationSeconds(String token) {
        return jwtProvider.getExpirationSeconds(token);
    }
}
