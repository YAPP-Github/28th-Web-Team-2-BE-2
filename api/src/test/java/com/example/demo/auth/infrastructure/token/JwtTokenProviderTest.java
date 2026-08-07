package com.example.demo.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.auth.application.result.TokenPayload;
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
                Duration.ofMinutes(30),
                Duration.ofDays(14));
    }

    @Test
    void Access와_Refresh_Token을_각각_생성하고_파싱한다() {
        final String accessToken = tokenProvider.createAccessToken("kakao-subject");
        final String refreshToken = tokenProvider.createRefreshToken("kakao-subject");

        final TokenPayload accessPayload = tokenProvider.parseAccessTokenPayload(accessToken);
        final TokenPayload refreshPayload = tokenProvider.parseRefreshTokenPayload(refreshToken);

        assertThat(accessPayload.subject()).isEqualTo("kakao-subject");
        assertThat(refreshPayload.subject()).isEqualTo("kakao-subject");
        assertThat(accessPayload.expiresAt()).isAfter(refreshPayload.expiresAt().minus(Duration.ofDays(14)));
    }

    @Test
    void 다른_종류의_Token은_파싱할_수_없다() {
        final String accessToken = tokenProvider.createAccessToken("subject");
        final String refreshToken = tokenProvider.createRefreshToken("subject");

        assertInvalid(() -> tokenProvider.parseRefreshTokenPayload(accessToken));
        assertInvalid(() -> tokenProvider.parseAccessTokenPayload(refreshToken));
    }

    @Test
    void 위조되거나_만료된_Token은_파싱할_수_없다() {
        assertInvalid(() -> tokenProvider.parseAccessTokenPayload("not-a-jwt"));

        final JwtTokenProvider expiredProvider = new JwtTokenProvider(
                "0123456789abcdef0123456789abcdef",
                Duration.ofSeconds(-1),
                Duration.ofSeconds(-1));
        assertInvalid(() -> expiredProvider.parseAccessTokenPayload(
                expiredProvider.createAccessToken("subject")));
    }

    private void assertInvalid(final Runnable action) {
        assertThatThrownBy(() -> action.run())
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_TOKEN));
    }
}
