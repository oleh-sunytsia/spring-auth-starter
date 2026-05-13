package com.benatti.auth.storage;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity that maps to the `refresh_tokens` table.
 * Used by JpaRefreshTokenStore when spring-boot-starter-data-jpa is on the classpath.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_rt_token",   columnList = "token"),
        @Index(name = "idx_rt_user_id", columnList = "userId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String userId;

    /** The raw JWT refresh token string */
    @Column(nullable = false, length = 2048, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private boolean revoked;

    private Instant revokedAt;

    /** Optional device info for multi-device management */
    @Column(length = 64)
    private String deviceId;

    @Column(length = 128)
    private String deviceName;

    private Instant lastUsedAt;
}
