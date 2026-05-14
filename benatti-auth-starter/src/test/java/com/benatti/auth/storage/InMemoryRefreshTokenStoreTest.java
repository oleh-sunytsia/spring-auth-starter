package com.benatti.auth.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InMemoryRefreshTokenStore")
class InMemoryRefreshTokenStoreTest {

    private InMemoryRefreshTokenStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRefreshTokenStore();
    }

    private RefreshToken validToken(String userId) {
        return RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .token("rt-" + UUID.randomUUID())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .revoked(false)
                .build();
    }

    @Nested
    @DisplayName("save & findByTokenAndValid")
    class SaveAndFind {

        @Test
        @DisplayName("saved token is found and returned")
        void foundAfterSave() {
            RefreshToken rt = validToken("user-1");
            store.save(rt);

            Optional<RefreshToken> found = store.findByTokenAndValid(rt.getToken());
            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("unknown token returns empty")
        void unknownTokenReturnsEmpty() {
            assertThat(store.findByTokenAndValid("no-such-token")).isEmpty();
        }

        @Test
        @DisplayName("assigns an ID if none provided")
        void assignsIdWhenMissing() {
            RefreshToken rt = RefreshToken.builder()
                    .userId("user-1")
                    .token("rt-abc")
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            rt.setId(null);
            store.save(rt);
            assertThat(rt.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findByTokenAndValid — exclusion conditions")
    class ExclusionConditions {

        @Test
        @DisplayName("revoked token returns empty")
        void revokedTokenReturnsEmpty() {
            RefreshToken rt = validToken("user-2");
            rt.setRevoked(true);
            store.save(rt);

            assertThat(store.findByTokenAndValid(rt.getToken())).isEmpty();
        }

        @Test
        @DisplayName("expired token returns empty")
        void expiredTokenReturnsEmpty() {
            RefreshToken rt = validToken("user-3");
            rt.setExpiresAt(Instant.now().minusSeconds(1));
            store.save(rt);

            assertThat(store.findByTokenAndValid(rt.getToken())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("returns all tokens for a given user")
        void returnsAllForUser() {
            RefreshToken rt1 = validToken("user-4");
            RefreshToken rt2 = validToken("user-4");
            RefreshToken rt3 = validToken("user-5");
            store.save(rt1);
            store.save(rt2);
            store.save(rt3);

            List<RefreshToken> tokens = store.findByUserId("user-4");
            assertThat(tokens).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list for unknown user")
        void emptyForUnknownUser() {
            assertThat(store.findByUserId("nobody")).isEmpty();
        }
    }

    @Nested
    @DisplayName("invalidate")
    class Invalidate {

        @Test
        @DisplayName("revoked token is no longer found as valid")
        void revokedTokenIsGone() {
            RefreshToken rt = validToken("user-6");
            store.save(rt);

            store.invalidate(rt.getToken());

            assertThat(store.findByTokenAndValid(rt.getToken())).isEmpty();
        }

        @Test
        @DisplayName("invalidating non-existent token does not throw")
        void noExceptionForUnknown() {
            assertThatNoException().isThrownBy(() -> store.invalidate("phantom-token"));
        }
    }

    @Nested
    @DisplayName("deleteExpired")
    class DeleteExpired {

        @Test
        @DisplayName("removes expired and revoked tokens; keeps valid ones")
        void removesExpiredAndRevoked() {
            RefreshToken valid   = validToken("user-7");
            RefreshToken expired = validToken("user-7");
            expired.setExpiresAt(Instant.now().minusSeconds(10));
            RefreshToken revoked = validToken("user-7");
            revoked.setRevoked(true);

            store.save(valid);
            store.save(expired);
            store.save(revoked);

            store.deleteExpired();

            assertThat(store.findByUserId("user-7")).hasSize(1);
            assertThat(store.findByUserId("user-7").get(0).getToken())
                    .isEqualTo(valid.getToken());
        }
    }
}
