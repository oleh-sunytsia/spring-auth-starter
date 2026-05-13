package com.benatti.auth.storage;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {
    
    /**
     * Save refresh token
     *
     * @param token token to save
     */
    void save(RefreshToken token);
    
    /**
     * Find valid token by string and check if it is not revoked
     *
     * @param token token string
     * @return Optional with token if found and valid
     */
    Optional<RefreshToken> findByTokenAndValid(String token);
    
    /**
     * Find all user tokens
     *
     * @param userId user ID
     * @return list of tokens
     */
    List<RefreshToken> findByUserId(String userId);
    
    /**
     * Revoke refresh token (forbid its use)
     *
     * @param token token string
     */
    void invalidate(String token);
    
    /**
     * Delete all expired tokens from database (cleanup)
     */
    void deleteExpired();
}
