package com.example.demo.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.RefreshTokenHasher;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReissueTokenUseCaseTest {

    @Test
    void 저장된_refresh_token은_현재_user_role로_새_토큰을_발급한다() {
        final TokenProvider tokenProvider = mock(TokenProvider.class);
        final RefreshTokenStore store = mock(RefreshTokenStore.class);
        final UserRepository userRepository = mock(UserRepository.class);
        final AuthTokenIssuer issuer = mock(AuthTokenIssuer.class);
        final RefreshTokenHasher hasher = new RefreshTokenHasher();
        final User user = mock(User.class);
        when(tokenProvider.parseRefreshTokenPayload("refresh-token"))
                .thenReturn(new TokenPayload(1L, Instant.now().plusSeconds(60)));
        when(store.consume(eq(1L), eq(hasher.hash("refresh-token")))).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.id()).thenReturn(1L);
        when(user.role()).thenReturn(UserRole.USER);
        when(issuer.issue(1L, UserRole.USER)).thenReturn(new AuthToken("access-token", "new-refresh-token"));
        final ReissueTokenUseCase useCase = new ReissueTokenUseCase(
                tokenProvider, store, hasher, issuer, userRepository);

        final AuthToken result = useCase.execute(new RefreshTokenCommand("refresh-token"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(issuer).issue(1L, UserRole.USER);
    }

    @Test
    void 이미_회전된_refresh_token은_재사용할_수_없다() {
        final TokenProvider tokenProvider = mock(TokenProvider.class);
        final RefreshTokenStore store = mock(RefreshTokenStore.class);
        final UserRepository userRepository = mock(UserRepository.class);
        final RefreshTokenHasher hasher = new RefreshTokenHasher();
        when(tokenProvider.parseRefreshTokenPayload("refresh-token"))
                .thenReturn(new TokenPayload(1L, Instant.now().plusSeconds(60)));
        when(store.consume(eq(1L), eq(hasher.hash("refresh-token")))).thenReturn(false);
        final ReissueTokenUseCase useCase = new ReissueTokenUseCase(
                tokenProvider, store, hasher, mock(AuthTokenIssuer.class), userRepository);

        assertThatThrownBy(() -> useCase.execute(new RefreshTokenCommand("refresh-token")))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_TOKEN));
        verifyNoInteractions(userRepository);
    }

    @Test
    void 없는_refresh_token은_401을_던진다() {
        final ReissueTokenUseCase useCase = new ReissueTokenUseCase(
                mock(TokenProvider.class),
                mock(RefreshTokenStore.class),
                new RefreshTokenHasher(),
                mock(AuthTokenIssuer.class),
                mock(UserRepository.class));

        assertThatThrownBy(() -> useCase.execute(new RefreshTokenCommand(null)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_TOKEN));
    }
}
