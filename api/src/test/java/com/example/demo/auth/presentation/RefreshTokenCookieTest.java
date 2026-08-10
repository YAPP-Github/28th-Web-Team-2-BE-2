package com.example.demo.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefreshTokenCookieTest {

    @Test
    void 생성된_쿠키는_설정된_수명과_보안_속성을_사용한다() {
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
    void 로컬_설정에서는_Secure를_끄도록_할_수_있다() {
        final RefreshTokenCookie cookie = new RefreshTokenCookie(false, Duration.ofDays(14));

        assertThat(cookie.create("refresh-token").toString()).doesNotContain("Secure");
    }

    @Test
    void 삭제_쿠키는_같은_경로에서_즉시_만료된다() {
        final RefreshTokenCookie cookie = new RefreshTokenCookie(true, Duration.ofDays(14));

        assertThat(cookie.delete().toString())
                .contains("refreshToken=")
                .contains("Path=/api/auth")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }
}
