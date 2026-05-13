package com.benatti.auth.exception;

import com.benatti.auth.jwt.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Translates library exceptions to RFC 9457 Problem Detail responses.
 * Registered automatically via @Import in AuthAutoConfiguration.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "invalid_credentials", ex.getMessage());
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ProblemDetail> handleRefreshTokenExpired(RefreshTokenExpiredException ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "refresh_token_expired", ex.getMessage());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ProblemDetail> handleTokenExpired(TokenExpiredException ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "token_expired", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ProblemDetail> handleJwtException(JwtException ex) {
        String code = ex.getErrorType() != null ? ex.getErrorType().name().toLowerCase() : "jwt_error";
        return problemDetail(HttpStatus.UNAUTHORIZED, code, ex.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> handleAuthException(AuthException ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage());
    }

    // ── helper ─────────────────────────────────────────────────────────────

    private ResponseEntity<ProblemDetail> problemDetail(HttpStatus status, String errorCode,
                                                        String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("about:blank"));
        pd.setProperty("errorCode", errorCode);
        pd.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(pd);
    }
}
