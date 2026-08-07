package com.example.demo.auth.application;

import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AuthToken;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenIssuer {

    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Duration refreshTokenExpiration;

    public AuthTokenIssuer(
            final TokenProvider tokenProvider,
            final RefreshTokenStore refreshTokenStore,
            final RefreshTokenHasher refreshTokenHasher,
            @Value("${jwt.refresh-token-expiration}") final Duration refreshTokenExpiration) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.refreshTokenHasher = refreshTokenHasher;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public AuthToken issue(final String subject) {
        final String accessToken = tokenProvider.createAccessToken(subject);
        final String refreshToken = tokenProvider.createRefreshToken(subject);
        refreshTokenStore.save(refreshTokenHasher.hash(refreshToken), subject, refreshTokenExpiration);
        return new AuthToken(accessToken, refreshToken);
    }
}
