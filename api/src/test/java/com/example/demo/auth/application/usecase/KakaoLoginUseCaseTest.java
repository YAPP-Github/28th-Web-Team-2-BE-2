package com.example.demo.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.RefreshTokenHasher;
import com.example.demo.auth.application.port.KakaoIdentityProvider;
import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.auth.application.command.KakaoLoginCommand;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KakaoLoginUseCaseTest {

    @Test
    void 유효한_idToken의_Kakao_subject로_앱_토큰을_발급한다() {
        final KakaoLoginUseCase useCase = new KakaoLoginUseCase(
                idToken -> "kakao-subject",
                new AuthTokenIssuer(
                        new FixedTokenProvider(),
                        new NoopRefreshTokenStore(),
                        new RefreshTokenHasher(),
                        Duration.ofDays(14)));

        final AuthToken result = useCase.execute(new KakaoLoginCommand("id-token"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void 빈_idToken은_Kakao_검증을_호출하지_않고_401을_던진다() {
        final KakaoLoginUseCase useCase = new KakaoLoginUseCase(
                idToken -> { throw new AssertionError("검증하면 안 됩니다."); },
                new AuthTokenIssuer(
                        new FixedTokenProvider(),
                        new NoopRefreshTokenStore(),
                        new RefreshTokenHasher(),
                        Duration.ofDays(14)));

        assertThatThrownBy(() -> useCase.execute(new KakaoLoginCommand(" ")))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.KAKAO_TOKEN_INVALID));
    }

    private static final class FixedTokenProvider implements TokenProvider {

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

    private static final class NoopRefreshTokenStore implements RefreshTokenStore {

        @Override
        public void save(final String tokenHash, final String subject, final Duration ttl) {}

        @Override
        public boolean consume(final String tokenHash, final String subject) {
            return false;
        }

        @Override
        public void delete(final String tokenHash) {}
    }
}
