package com.example.demo.auth.application.usecase;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.RefreshTokenHasher;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.auth.domain.User;
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
    private final UserRepository userRepository;

    public ReissueTokenUseCase(
            final TokenProvider tokenProvider,
            final RefreshTokenStore refreshTokenStore,
            final RefreshTokenHasher refreshTokenHasher,
            final AuthTokenIssuer authTokenIssuer,
            final UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.refreshTokenHasher = refreshTokenHasher;
        this.authTokenIssuer = authTokenIssuer;
        this.userRepository = userRepository;
    }

    public AuthToken execute(final RefreshTokenCommand command) {
        if (!StringUtils.hasText(command.refreshToken())) {
            throw invalidToken();
        }

        final String refreshToken = command.refreshToken();
        final TokenPayload payload = tokenProvider.parseRefreshTokenPayload(refreshToken);
        final boolean matches = refreshTokenStore.matches(
                payload.userId(), refreshTokenHasher.hash(refreshToken));
        if (!matches) {
            throw invalidToken();
        }
        final User user = userRepository.findById(payload.userId()).orElseThrow(this::invalidToken);
        return authTokenIssuer.issue(user.id(), user.role());
    }

    private ApiException invalidToken() {
        return new ApiException(ErrorType.INVALID_TOKEN.description(), ErrorType.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
    }
}
