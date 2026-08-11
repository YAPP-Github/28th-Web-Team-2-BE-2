package com.example.demo.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.auth.domain.UserRole;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthTokenIssuerTest {

    @Test
    void 발급한_refresh_token은_원문이_아닌_해시로_Redis_저장소에_저장한다() {
        final RecordingTokenProvider tokenProvider = new RecordingTokenProvider();
        final RecordingRefreshTokenStore store = new RecordingRefreshTokenStore();
        final AuthTokenIssuer issuer = new AuthTokenIssuer(
                tokenProvider, store, new RefreshTokenHasher(), Duration.ofDays(14));

        final var token = issuer.issue(1L, UserRole.USER);

        assertThat(token.accessToken()).isEqualTo("access-token");
        assertThat(token.refreshToken()).isEqualTo("refresh-token");
        assertThat(store.userId()).isEqualTo(1L);
        assertThat(store.tokenHash()).isNotEqualTo(token.refreshToken()).hasSize(64);
        assertThat(store.ttl()).isEqualTo(Duration.ofDays(14));
    }

    private static final class RecordingTokenProvider implements TokenProvider {

        @Override
        public String createAccessToken(final Long userId, final UserRole role) {
            return "access-token";
        }

        @Override
        public String createRefreshToken(final Long userId) {
            return "refresh-token";
        }

        @Override
        public AccessTokenPayload parseAccessTokenPayload(final String token) {
            return new AccessTokenPayload(1L, UserRole.USER);
        }

        @Override
        public TokenPayload parseRefreshTokenPayload(final String token) {
            return new TokenPayload(1L, Instant.now().plusSeconds(60));
        }
    }

    private static final class RecordingRefreshTokenStore implements RefreshTokenStore {

        private Long userId;
        private String tokenHash;
        private Duration ttl;

        @Override
        public void save(final Long userId, final String tokenHash, final Duration ttl) {
            this.userId = userId;
            this.tokenHash = tokenHash;
            this.ttl = ttl;
        }

        @Override
        public boolean consume(final Long userId, final String tokenHash) {
            return false;
        }

        @Override
        public void delete(final Long userId) {}

        private Long userId() {
            return userId;
        }

        private String tokenHash() {
            return tokenHash;
        }

        private Duration ttl() {
            return ttl;
        }
    }
}
