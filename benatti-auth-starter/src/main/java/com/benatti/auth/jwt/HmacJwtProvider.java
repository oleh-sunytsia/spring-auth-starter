package com.benatti.auth.jwt;

import com.benatti.auth.autoconfigure.AuthProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Default HMAC-SHA256 JWT provider.
 * Replace with RsaJwtProvider for asymmetric key scenarios.
 */
public class HmacJwtProvider implements JwtProvider {

    private static final String CLAIM_USERNAME    = "username";
    private static final String CLAIM_EMAIL       = "email";
    private static final String CLAIM_ROLES       = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_TYPE        = "type";

    private final SecretKey secretKey;
    private final String    issuer;

    public HmacJwtProvider(AuthProperties properties) {
        byte[] keyBytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer    = properties.getTokenIssuer();
    }

    @Override
    public String generateToken(JwtPayload payload) throws JwtException {
        try {
            Map<String, Object> claims = new HashMap<>();
            if (payload.getUsername()    != null) claims.put(CLAIM_USERNAME,    payload.getUsername());
            if (payload.getEmail()       != null) claims.put(CLAIM_EMAIL,       payload.getEmail());
            if (payload.getRoles()       != null) claims.put(CLAIM_ROLES,       payload.getRoles());
            if (payload.getPermissions() != null) claims.put(CLAIM_PERMISSIONS, payload.getPermissions());
            if (payload.getType()        != null) claims.put(CLAIM_TYPE,        payload.getType());
            if (payload.getCustomClaims() != null) claims.putAll(payload.getCustomClaims());

            return Jwts.builder()
                    .claims(claims)
                    .subject(payload.getSubject())
                    .issuer(payload.getIssuer() != null ? payload.getIssuer() : issuer)
                    .issuedAt(toDate(payload.getIssuedAt()))
                    .expiration(toDate(payload.getExpiresAt()))
                    .signWith(secretKey)
                    .compact();

        } catch (Exception e) {
            throw new JwtException("Failed to generate JWT token: " + e.getMessage(),
                    JwtException.ErrorType.MALFORMED);
        }
    }

    @Override
    public JwtPayload validateAndParseToken(String token) throws JwtException {
        Claims claims = parseClaims(token);

        return JwtPayload.builder()
                .subject(claims.getSubject())
                .username(claims.get(CLAIM_USERNAME, String.class))
                .email(claims.get(CLAIM_EMAIL, String.class))
                .roles(castList(claims.get(CLAIM_ROLES)))
                .permissions(castList(claims.get(CLAIM_PERMISSIONS)))
                .type(claims.get(CLAIM_TYPE, String.class))
                .issuedAt(toInstant(claims.getIssuedAt()))
                .expiresAt(toInstant(claims.getExpiration()))
                .issuer(claims.getIssuer())
                .build();
    }

    @Override
    public boolean isValidToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    @Override
    public long getExpirationSeconds(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining / 1000, 0);
        } catch (JwtException e) {
            return 0;
        }
    }

    // ── private helpers ────────────────────────────────────────────────────

    private Claims parseClaims(String token) throws JwtException {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token has expired", JwtException.ErrorType.EXPIRED);
        } catch (SignatureException e) {
            throw new JwtException("Invalid JWT signature", JwtException.ErrorType.INVALID_SIGNATURE);
        } catch (MalformedJwtException e) {
            throw new JwtException("Malformed JWT token", JwtException.ErrorType.MALFORMED);
        } catch (UnsupportedJwtException e) {
            throw new JwtException("Unsupported JWT token", JwtException.ErrorType.UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            throw new JwtException("JWT claims string is empty", JwtException.ErrorType.CLAIMS_EMPTY);
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.List<String> castList(Object value) {
        if (value instanceof java.util.List<?> list) {
            return (java.util.List<String>) list;
        }
        return java.util.Collections.emptyList();
    }

    private Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }

    private Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }
}
