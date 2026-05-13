package com.benatti.auth.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for RefreshTokenEntity.
 * Used internally by JpaRefreshTokenStore.
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, String> {

    Optional<RefreshTokenEntity> findByTokenAndRevokedFalse(String token);

    List<RefreshTokenEntity> findByUserId(String userId);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true, r.revokedAt = :now WHERE r.token = :token")
    int revokeByToken(@Param("token") String token, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.revoked = true OR r.expiresAt < :now")
    int deleteExpiredAndRevoked(@Param("now") Instant now);
}
