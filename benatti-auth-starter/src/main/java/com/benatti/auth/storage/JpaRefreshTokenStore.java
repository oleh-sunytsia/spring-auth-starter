package com.benatti.auth.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA-backed implementation of RefreshTokenRepository.
 * Activated automatically when RefreshTokenJpaRepository bean is present
 * and no other RefreshTokenRepository bean is defined.
 */
@Component
@ConditionalOnBean(RefreshTokenJpaRepository.class)
@ConditionalOnMissingBean(InMemoryRefreshTokenStore.class)
@Transactional
public class JpaRefreshTokenStore implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public JpaRefreshTokenStore(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(RefreshToken token) {
        RefreshTokenEntity entity = toEntity(token);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        jpaRepository.save(entity);
        token.setId(entity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenAndValid(String token) {
        return jpaRepository.findByTokenAndRevokedFalse(token)
                .filter(e -> e.getExpiresAt() == null || e.getExpiresAt().isAfter(Instant.now()))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void invalidate(String token) {
        jpaRepository.revokeByToken(token, Instant.now());
    }

    @Override
    public void deleteExpired() {
        jpaRepository.deleteExpiredAndRevoked(Instant.now());
    }

    // ── mapping ────────────────────────────────────────────────────────────

    private RefreshTokenEntity toEntity(RefreshToken t) {
        return RefreshTokenEntity.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .token(t.getToken())
                .createdAt(t.getCreatedAt())
                .expiresAt(t.getExpiresAt())
                .revoked(t.isRevoked())
                .revokedAt(t.getRevokedAt())
                .deviceId(t.getDeviceId())
                .deviceName(t.getDeviceName())
                .lastUsedAt(t.getLastUsedAt())
                .build();
    }

    private RefreshToken toDomain(RefreshTokenEntity e) {
        return RefreshToken.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .token(e.getToken())
                .createdAt(e.getCreatedAt())
                .expiresAt(e.getExpiresAt())
                .revoked(e.isRevoked())
                .revokedAt(e.getRevokedAt())
                .deviceId(e.getDeviceId())
                .deviceName(e.getDeviceName())
                .lastUsedAt(e.getLastUsedAt())
                .build();
    }
}
