package com.example.demo.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "0123456789abcdef0123456789abcdef",
                "abcdef0123456789abcdef0123456789",
                Duration.ofMinutes(30),
                Duration.ofDays(14));
    }

    @Test
    void Access와_Refresh_Token은_내부_user_id를_담고_서로_다른_키로_검증한다() {
        final String accessToken = tokenProvider.createAccessToken(1L, UserRole.USER);
        final String refreshToken = tokenProvider.createRefreshToken(1L);

        final AccessTokenPayload accessPayload = tokenProvider.parseAccessTokenPayload(accessToken);
        final TokenPayload refreshPayload = tokenProvider.parseRefreshTokenPayload(refreshToken);

        assertThat(accessPayload.userId()).isEqualTo(1L);
        assertThat(accessPayload.role()).isEqualTo(UserRole.USER);
        assertThat(refreshPayload.userId()).isEqualTo(1L);
        assertThat(accessPayload.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void 다른_종류의_Token은_파싱할_수_없다() {
        final String accessToken = tokenProvider.createAccessToken(1L, UserRole.USER);
        final String refreshToken = tokenProvider.createRefreshToken(1L);

        assertInvalid(() -> tokenProvider.parseRefreshTokenPayload(accessToken));
        assertInvalid(() -> tokenProvider.parseAccessTokenPayload(refreshToken));
    }

    @Test
    void 위조되거나_만료된_Token은_파싱할_수_없다() {
        assertInvalid(() -> tokenProvider.parseAccessTokenPayload("not-a-jwt"));

        final JwtTokenProvider expiredProvider = new JwtTokenProvider(
                "0123456789abcdef0123456789abcdef",
                "abcdef0123456789abcdef0123456789",
                Duration.ofSeconds(-1),
                Duration.ofSeconds(-1));
        assertInvalid(() -> expiredProvider.parseAccessTokenPayload(
                expiredProvider.createAccessToken(1L, UserRole.USER)));
    }

    private void assertInvalid(final Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_TOKEN));
    }
}
