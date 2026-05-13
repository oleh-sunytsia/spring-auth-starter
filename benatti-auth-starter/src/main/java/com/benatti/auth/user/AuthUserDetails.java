package com.benatti.auth.user;

import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;

public interface AuthUserDetails extends UserDetails {
    
    /**
     * Get user ID
     */
    String getUserId();
    
    /**
     * Get user email
     */
    String getEmail();
    
    /**
     * Get user permissions
     * (for example: READ:USERS, DELETE:POSTS)
     */
    List<String> getPermissions();
    
    /**
     * Get last login time
     */
    Instant getLastLogin();
}
