package com.example.demo.auth.application;

import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.domain.UserRole;
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

    public AuthToken issue(final Long userId, final UserRole role) {
        final String accessToken = tokenProvider.createAccessToken(userId, role);
        final String refreshToken = tokenProvider.createRefreshToken(userId);
        refreshTokenStore.save(userId, refreshTokenHasher.hash(refreshToken), refreshTokenExpiration);
        return new AuthToken(accessToken, refreshToken);
    }
}
