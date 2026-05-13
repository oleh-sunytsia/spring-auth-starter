package com.benatti.auth.auth;

import com.benatti.auth.autoconfigure.AuthProperties;
import com.benatti.auth.dto.AuthResponse;
import com.benatti.auth.dto.UserDTO;
import com.benatti.auth.event.LoginFailureEvent;
import com.benatti.auth.event.LoginSuccessEvent;
import com.benatti.auth.event.LogoutEvent;
import com.benatti.auth.event.TokenRefreshEvent;
import com.benatti.auth.exception.AuthException;
import com.benatti.auth.exception.InvalidCredentialsException;
import com.benatti.auth.exception.RefreshTokenExpiredException;
import com.benatti.auth.jwt.JwtException;
import com.benatti.auth.jwt.JwtPayload;
import com.benatti.auth.jwt.JwtTokenStore;
import com.benatti.auth.storage.RefreshToken;
import com.benatti.auth.storage.RefreshTokenRepository;
import com.benatti.auth.user.AuthUserDetails;
import com.benatti.auth.user.UserDetailsProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default, production-ready implementation of AuthService.
 * Developers may replace this bean by declaring their own AuthService bean.
 */
public class DefaultAuthService implements AuthService {

    private final AuthenticationManager   authenticationManager;
    private final UserDetailsProvider     userDetailsProvider;
    private final JwtTokenStore           jwtTokenStore;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties          properties;

    public DefaultAuthService(AuthenticationManager authenticationManager,
                              UserDetailsProvider userDetailsProvider,
                              JwtTokenStore jwtTokenStore,
                              RefreshTokenRepository refreshTokenRepository,
                              ApplicationEventPublisher eventPublisher,
                              AuthProperties properties) {
        this.authenticationManager  = authenticationManager;
        this.userDetailsProvider    = userDetailsProvider;
        this.jwtTokenStore          = jwtTokenStore;
        this.refreshTokenRepository = refreshTokenRepository;
        this.eventPublisher         = eventPublisher;
        this.properties             = properties;
    }

    // ── AuthService ────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(String username, String password) throws AuthException {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            AuthUserDetails userDetails = userDetailsProvider.loadUserByUsername(username);

            String accessToken  = jwtTokenStore.generateAccessToken(userDetails);
            String refreshToken = jwtTokenStore.generateRefreshToken(userDetails);

            persistRefreshToken(userDetails.getUserId(), refreshToken);

            eventPublisher.publishEvent(
                    new LoginSuccessEvent(this, userDetails.getUserId(), username));

            return buildResponse(accessToken, refreshToken, userDetails);

        } catch (BadCredentialsException e) {
            eventPublisher.publishEvent(new LoginFailureEvent(this, username, "Invalid credentials"));
            throw new InvalidCredentialsException("Invalid username or password");
        } catch (DisabledException e) {
            eventPublisher.publishEvent(new LoginFailureEvent(this, username, "Account disabled"));
            throw new AuthException("Account is disabled");
        } catch (LockedException e) {
            eventPublisher.publishEvent(new LoginFailureEvent(this, username, "Account locked"));
            throw new AuthException("Account is locked");
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) throws AuthException {
        JwtPayload payload;
        try {
            payload = jwtTokenStore.validateToken(refreshToken);
        } catch (JwtException e) {
            throw new RefreshTokenExpiredException("Invalid or expired refresh token");
        }

        if (!"refresh".equals(payload.getType())) {
            throw new RefreshTokenExpiredException("Token is not a refresh token");
        }

        Optional<RefreshToken> stored = refreshTokenRepository.findByTokenAndValid(refreshToken);
        if (stored.isEmpty()) {
            throw new RefreshTokenExpiredException("Refresh token not found or revoked");
        }

        AuthUserDetails userDetails;
        try {
            userDetails = userDetailsProvider.loadUserById(payload.getSubject());
        } catch (UserDetailsProvider.UserNotFoundException e) {
            throw new AuthException("User associated with token no longer exists");
        }

        String newAccessToken = jwtTokenStore.generateAccessToken(userDetails);

        eventPublisher.publishEvent(new TokenRefreshEvent(this, userDetails.getUserId()));

        return buildResponse(newAccessToken, refreshToken, userDetails);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenStore.isValidToken(token);
    }

    @Override
    public void logout(String userId, String refreshToken) {
        refreshTokenRepository.invalidate(refreshToken);
        eventPublisher.publishEvent(new LogoutEvent(this, userId));
    }

    @Override
    public AuthUserDetails getUserDetailsFromToken(String token) throws AuthException {
        try {
            JwtPayload payload = jwtTokenStore.validateToken(token);
            return userDetailsProvider.loadUserById(payload.getSubject());
        } catch (JwtException | UserDetailsProvider.UserNotFoundException e) {
            throw new AuthException("Invalid token: " + e.getMessage());
        }
    }

    // ── private helpers ────────────────────────────────────────────────────

    private void persistRefreshToken(String userId, String token) {
        RefreshToken rt = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .token(token)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(
                        Duration.ofDays(properties.getRefreshTokenExpirationDays())))
                .revoked(false)
                .build();
        refreshTokenRepository.save(rt);
    }

    private AuthResponse buildResponse(String accessToken, String refreshToken,
                                       AuthUserDetails user) {
        UserDTO userDTO = UserDTO.builder()
                .id(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .permissions(user.getPermissions())
                .lastLogin(user.getLastLogin())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(properties.getAccessTokenExpirationMinutes() * 60)
                .user(userDTO)
                .build();
    }
}
