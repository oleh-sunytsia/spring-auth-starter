package com.benatti.auth.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    
    /** Token ID */
    private String id;
    
    /** User ID */
    private String userId;
    
    /** Token itself */
    private String token;
    
    /** Created at time */
    private Instant createdAt;
    
    /** Expires at time */
    private Instant expiresAt;
    
    /** Is token revoked */
    private boolean revoked;
    
    /** Revoked at time */
    private Instant revokedAt;
    
    /** Device ID (optional) */
    private String deviceId;
    
    /** Device name (optional) */
    private String deviceName;
    
    /** Last used at time */
    private Instant lastUsedAt;
}
