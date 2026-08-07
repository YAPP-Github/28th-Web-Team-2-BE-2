package com.example.demo.auth.application.usecase;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.RefreshTokenHasher;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReissueTokenUseCase {

    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenHasher refreshTokenHasher;
    private final AuthTokenIssuer authTokenIssuer;

    public ReissueTokenUseCase(
            final TokenProvider tokenProvider,
            final RefreshTokenStore refreshTokenStore,
            final RefreshTokenHasher refreshTokenHasher,
            final AuthTokenIssuer authTokenIssuer) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.refreshTokenHasher = refreshTokenHasher;
        this.authTokenIssuer = authTokenIssuer;
    }

    public AuthToken execute(final RefreshTokenCommand command) {
        if (!StringUtils.hasText(command.refreshToken())) {
            throw invalidToken();
        }

        final String refreshToken = command.refreshToken();
        final var payload = tokenProvider.parseRefreshTokenPayload(refreshToken);
        final boolean consumed = refreshTokenStore.consume(
                refreshTokenHasher.hash(refreshToken), payload.subject());
        if (!consumed) {
            throw invalidToken();
        }
        return authTokenIssuer.issue(payload.subject());
    }

    private ApiException invalidToken() {
        return new ApiException(ErrorType.INVALID_TOKEN.description(), ErrorType.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
    }
}
