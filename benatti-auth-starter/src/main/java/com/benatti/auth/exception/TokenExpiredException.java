package com.benatti.auth.exception;

public class TokenExpiredException extends AuthException {
    
    public TokenExpiredException(String message) {
        super(message, "TOKEN_EXPIRED");
    }
    
    public TokenExpiredException(String message, Throwable cause) {
        super(message, "TOKEN_EXPIRED", cause);
    }
}
