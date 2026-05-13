package com.benatti.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse implements Serializable {
    
    /** JWT access token (short-lived) */
    private String accessToken;
    
    /** JWT refresh token (long-lived) */
    private String refreshToken;
    
    /** Access token expiration time in seconds */
    private long expiresIn;
    
    /** User information */
    private UserDTO user;
}
