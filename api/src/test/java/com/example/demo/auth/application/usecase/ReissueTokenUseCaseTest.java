package com.example.demo.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.RefreshTokenHasher;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReissueTokenUseCaseTest {

    @Test
    void 저장된_refresh_token은_소비한_뒤_새_토큰으로_회전한다() {
        final RecordingStore store = new RecordingStore(true);
        final ReissueTokenUseCase useCase = new ReissueTokenUseCase(
                new FixedTokenProvider(),
                store,
                new RefreshTokenHasher(),
                new AuthTokenIssuer(
                        new FixedTokenProvider(), store, new RefreshTokenHasher(), Duration.ofDays(14)));

        final AuthToken result = useCase.execute(new RefreshTokenCommand("refresh-token"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(store.consumedSubject()).isEqualTo("kakao-subject");
    }

    @Test
    void 이미_소비된_refresh_token은_재사용할_수_없다() {
        final RecordingStore store = new RecordingStore(false);
        final ReissueTokenUseCase useCase = new ReissueTokenUseCase(
                new FixedTokenProvider(),
                store,
                new RefreshTokenHasher(),
                new AuthTokenIssuer(
                        new FixedTokenProvider(), store, new RefreshTokenHasher(), Duration.ofDays(14)));

        assertThatThrownBy(() -> useCase.execute(new RefreshTokenCommand("refresh-token")))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_TOKEN));
    }

    @Test
    void 없는_refresh_token은_401을_던진다() {
        final ReissueTokenUseCase useCase = new ReissueTokenUseCase(
                new FixedTokenProvider(),
                new RecordingStore(true),
                new RefreshTokenHasher(),
                new AuthTokenIssuer(
                        new FixedTokenProvider(), new RecordingStore(true), new RefreshTokenHasher(), Duration.ofDays(14)));

        assertThatThrownBy(() -> useCase.execute(new RefreshTokenCommand(null)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_TOKEN));
    }

    private static final class FixedTokenProvider implements TokenProvider {

        @Override
        public String createAccessToken(final String subject) {
            return "access-token";
        }

        @Override
        public String createRefreshToken(final String subject) {
            return "new-refresh-token";
        }

        @Override
        public TokenPayload parseAccessTokenPayload(final String token) {
            return new TokenPayload("kakao-subject", Instant.now().plusSeconds(60));
        }

        @Override
        public TokenPayload parseRefreshTokenPayload(final String token) {
            return new TokenPayload("kakao-subject", Instant.now().plusSeconds(60));
        }
    }

    private static final class RecordingStore implements RefreshTokenStore {

        private final boolean consumeResult;
        private String consumedSubject;

        private RecordingStore(final boolean consumeResult) {
            this.consumeResult = consumeResult;
        }

        @Override
        public void save(final String tokenHash, final String subject, final Duration ttl) {}

        @Override
        public boolean consume(final String tokenHash, final String subject) {
            consumedSubject = subject;
            return consumeResult;
        }

        @Override
        public void delete(final String tokenHash) {}

        private String consumedSubject() {
            return consumedSubject;
        }
    }
}
