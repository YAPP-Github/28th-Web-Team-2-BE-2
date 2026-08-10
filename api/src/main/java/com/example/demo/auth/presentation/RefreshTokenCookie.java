package com.example.demo.auth.presentation;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookie {

    public static final String NAME = "refreshToken";
    private static final String PATH = "/api/auth";
    private static final String SAME_SITE = "Lax";

    private final boolean secure;
    private final Duration maxAge;

    public RefreshTokenCookie(
            @Value("${jwt.refresh-cookie.secure:true}") final boolean secure,
            @Value("${jwt.refresh-token-expiration}") final Duration maxAge) {
        this.secure = secure;
        this.maxAge = maxAge;
    }

    public ResponseCookie create(final String refreshToken) {
        return base(refreshToken).maxAge(maxAge).build();
    }

    public ResponseCookie delete() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(final String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(PATH);
    }
}
