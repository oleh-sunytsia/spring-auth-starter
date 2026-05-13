package com.benatti.auth.auth;

import com.benatti.auth.dto.AuthResponse;
import com.benatti.auth.exception.AuthException;
import com.benatti.auth.user.AuthUserDetails;

public interface AuthService {
    
    /**
     * Authenticate user by username and password
     *
     * @param username username
     * @param password password
     * @return AuthResponse with tokens and user information
     * @throws AuthException if credentials are invalid
     */
    AuthResponse login(String username, String password) throws AuthException;
    
    /**
     * Refresh access token using refresh token
     *
     * @param refreshToken refresh token
     * @return new tokens
     * @throws AuthException if token expired or invalid
     */
    AuthResponse refreshToken(String refreshToken) throws AuthException;
    
    /**
     * Check if access token is valid
     *
     * @param token JWT token
     * @return true if valid
     */
    boolean validateToken(String token);
    
    /**
     * Logout user (revoke tokens)
     *
     * @param userId user ID
     * @param refreshToken refresh token to revoke
     */
    void logout(String userId, String refreshToken);
    
    /**
     * Get user details from token
     *
     * @param token JWT token
     * @return user details
     * @throws AuthException if token is invalid
     */
    AuthUserDetails getUserDetailsFromToken(String token) throws AuthException;
}
