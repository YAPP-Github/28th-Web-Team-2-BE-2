package com.example.demo.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.TokenPayload;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthTokenIssuerTest {

    @Test
    void 발급한_refresh_token은_원문이_아닌_해시로_Redis_저장소에_저장한다() {
        final RecordingTokenProvider tokenProvider = new RecordingTokenProvider();
        final RecordingRefreshTokenStore store = new RecordingRefreshTokenStore();
        final AuthTokenIssuer issuer = new AuthTokenIssuer(
                tokenProvider, store, new RefreshTokenHasher(), Duration.ofDays(14));

        final var token = issuer.issue("kakao-subject");

        assertThat(token.accessToken()).isEqualTo("access-token");
        assertThat(token.refreshToken()).isEqualTo("refresh-token");
        assertThat(store.tokenHash()).isNotEqualTo(token.refreshToken()).hasSize(64);
        assertThat(store.subject()).isEqualTo("kakao-subject");
        assertThat(store.ttl()).isEqualTo(Duration.ofDays(14));
    }

    private static final class RecordingTokenProvider implements TokenProvider {

        @Override
        public String createAccessToken(final String subject) {
            return "access-token";
        }

        @Override
        public String createRefreshToken(final String subject) {
            return "refresh-token";
        }

        @Override
        public TokenPayload parseAccessTokenPayload(final String token) {
            return new TokenPayload("subject", null);
        }

        @Override
        public TokenPayload parseRefreshTokenPayload(final String token) {
            return new TokenPayload("subject", null);
        }
    }

    private static final class RecordingRefreshTokenStore implements RefreshTokenStore {

        private String tokenHash;
        private String subject;
        private Duration ttl;

        @Override
        public void save(final String tokenHash, final String subject, final Duration ttl) {
            this.tokenHash = tokenHash;
            this.subject = subject;
            this.ttl = ttl;
        }

        @Override
        public boolean consume(final String tokenHash, final String subject) {
            return false;
        }

        @Override
        public void delete(final String tokenHash) {}

        private String tokenHash() {
            return tokenHash;
        }

        private String subject() {
            return subject;
        }

        private Duration ttl() {
            return ttl;
        }
    }
}
