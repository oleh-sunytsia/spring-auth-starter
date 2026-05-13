package com.benatti.auth.jwt;

public class JwtException extends RuntimeException {
    
    public enum ErrorType {
        INVALID_SIGNATURE, EXPIRED, MALFORMED, UNSUPPORTED, CLAIMS_EMPTY
    }
    
    private final ErrorType errorType;
    
    public JwtException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }
    
    public JwtException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
}
