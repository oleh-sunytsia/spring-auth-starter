package com.benatti.auth.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtPayload implements Serializable {
    
    /** Subject (User ID) */
    private String subject;
    
    /** Username */
    private String username;
    
    /** Email */
    private String email;
    
    /** Roles */
    private List<String> roles;
    
    /** Permissions */
    private List<String> permissions;
    
    /** Issued at time (iat) */
    private Instant issuedAt;
    
    /** Expiration time (exp) */
    private Instant expiresAt;
    
    /** Issuer (iss) */
    private String issuer;
    
    /** Token type: "access" or "refresh" */
    private String type;
    
    /** Custom claims */
    private Map<String, Object> customClaims;
}
