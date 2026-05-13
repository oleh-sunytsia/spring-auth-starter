package com.benatti.auth.controller;

import com.benatti.auth.auth.AuthService;
import com.benatti.auth.autoconfigure.AuthProperties;
import com.benatti.auth.dto.AuthResponse;
import com.benatti.auth.dto.LoginRequest;
import com.benatti.auth.dto.RefreshTokenRequest;
import com.benatti.auth.dto.ValidateTokenResponse;
import com.benatti.auth.exception.AuthException;
import com.benatti.auth.user.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Built-in authentication REST controller.
 * Endpoints are mounted under {@code auth.base-path} (default: /api/auth).
 */
@RestController
public class AuthController {

    private final AuthService    authService;
    private final AuthProperties properties;

    public AuthController(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties  = properties;
    }

    /**
     * POST /api/auth/login
     * Authenticate with username + password and receive JWT tokens.
     */
    @PostMapping("${auth.base-path:/api/auth}/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh
     * Exchange a valid refresh token for a new access token.
     */
    @PostMapping("${auth.base-path:/api/auth}/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Revoke the supplied refresh token.
     */
    @PostMapping("${auth.base-path:/api/auth}/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        // Resolve userId from token without failing hard – best-effort
        String userId = null;
        try {
            AuthUserDetails userDetails = authService.getUserDetailsFromToken(request.getRefreshToken());
            userId = userDetails.getUserId();
        } catch (AuthException ignored) { }

        authService.logout(userId, request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/auth/validate
     * Validate an access token passed as Bearer or query param.
     */
    @GetMapping("${auth.base-path:/api/auth}/validate")
    public ResponseEntity<ValidateTokenResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {

        String token = extractToken(authHeader, tokenParam);

        if (token == null || !authService.validateToken(token)) {
            return ResponseEntity.ok(ValidateTokenResponse.builder().valid(false).build());
        }

        try {
            AuthUserDetails userDetails = authService.getUserDetailsFromToken(token);
            ValidateTokenResponse resp = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId(userDetails.getUserId())
                    .roles(userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()))
                    .permissions(userDetails.getPermissions())
                    .build();
            return ResponseEntity.ok(resp);
        } catch (AuthException e) {
            return ResponseEntity.ok(ValidateTokenResponse.builder().valid(false).build());
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private String extractToken(String authHeader, String tokenParam) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return tokenParam;
    }
}
