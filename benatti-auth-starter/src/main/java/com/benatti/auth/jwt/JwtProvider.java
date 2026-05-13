package com.benatti.auth.jwt;

/**
 * Interface for generating and validating JWT tokens
 * 
 * Possible implementations:
 * - HmacJwtProvider (HMAC-SHA256) - default
 * - RsaJwtProvider (RSA-256) - for asymmetric encryption
 * - CustomJwtProvider - for special needs
 */
public interface JwtProvider {
    
    /**
     * Generate JWT token based on JwtPayload
     *
     * @param payload data for token
     * @return encoded JWT token
     * @throws JwtException if error during generation
     */
    String generateToken(JwtPayload payload) throws JwtException;
    
    /**
     * Validate and parse JWT token
     *
     * @param token encoded token
     * @return parsed JwtPayload
     * @throws JwtException if error during validation
     */
    JwtPayload validateAndParseToken(String token) throws JwtException;
    
    /**
     * Check if token is valid (signature and expiration date)
     *
     * @param token encoded token
     * @return true if valid, false otherwise
     */
    boolean isValidToken(String token);
    
    /**
     * Get remaining time until token expiration (in seconds)
     *
     * @param token encoded token
     * @return time in seconds
     */
    long getExpirationSeconds(String token);
}
