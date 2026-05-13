package com.benatti.auth.exception;

public class RefreshTokenExpiredException extends AuthException {
    
    public RefreshTokenExpiredException(String message) {
        super(message, "REFRESH_TOKEN_EXPIRED");
    }
    
    public RefreshTokenExpiredException(String message, Throwable cause) {
        super(message, "REFRESH_TOKEN_EXPIRED", cause);
    }
}
