package com.benatti.auth.exception;

public class UnauthorizedException extends AuthException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, "UNAUTHORIZED", cause);
    }
}
