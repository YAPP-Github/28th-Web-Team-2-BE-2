package com.example.demo.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefreshTokenCookieTest {

    @Test
    void 생성_cookie는_보안_속성과_경로를_갖는다() {
        final RefreshTokenCookie cookie = new RefreshTokenCookie(true, Duration.ofDays(14));

        assertThat(cookie.create("refresh-token").toString())
                .contains("refreshToken=refresh-token")
                .contains("Path=/api/auth")
                .contains("Max-Age=1209600")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }

    @Test
    void 삭제_cookie는_만료된다() {
        final RefreshTokenCookie cookie = new RefreshTokenCookie(true, Duration.ofDays(14));

        assertThat(cookie.delete().toString())
                .contains("refreshToken=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }
}
