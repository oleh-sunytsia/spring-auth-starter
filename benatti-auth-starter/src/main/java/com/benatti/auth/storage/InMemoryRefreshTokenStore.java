package com.benatti.auth.storage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe, in-memory implementation of RefreshTokenRepository.
 * Suitable for development and testing. Not recommended for multi-instance production deployments.
 */
public class InMemoryRefreshTokenStore implements RefreshTokenRepository {

    private final Map<String, RefreshToken> store = new ConcurrentHashMap<>();

    @Override
    public void save(RefreshToken token) {
        if (token.getId() == null) {
            token.setId(UUID.randomUUID().toString());
        }
        store.put(token.getToken(), token);
    }

    @Override
    public Optional<RefreshToken> findByTokenAndValid(String token) {
        RefreshToken rt = store.get(token);
        if (rt == null || rt.isRevoked()) {
            return Optional.empty();
        }
        if (rt.getExpiresAt() != null && rt.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(rt);
    }

    @Override
    public List<RefreshToken> findByUserId(String userId) {
        return store.values().stream()
                .filter(rt -> userId.equals(rt.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public void invalidate(String token) {
        RefreshToken rt = store.get(token);
        if (rt != null) {
            rt.setRevoked(true);
            rt.setRevokedAt(Instant.now());
        }
    }

    @Override
    public void deleteExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(entry -> {
            RefreshToken rt = entry.getValue();
            return rt.isRevoked()
                    || (rt.getExpiresAt() != null && rt.getExpiresAt().isBefore(now));
        });
    }
}
