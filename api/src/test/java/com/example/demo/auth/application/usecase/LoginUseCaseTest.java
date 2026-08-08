package com.example.demo.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.RefreshTokenHasher;
import com.example.demo.auth.application.command.LoginCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.application.result.OAuthUserInfo;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LoginUseCaseTest {

    @Test
    void 신규_Kakao_사용자를_생성하고_내부_user_id로_토큰을_발급한다() {
        final UserRepository userRepository = mock(UserRepository.class);
        final User user = user(1L);
        when(userRepository.findByProviderAndProviderSubject(any(), any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        final LoginUseCase useCase = new LoginUseCase(
                idToken -> new OAuthUserInfo("kakao-subject", "user@example.com", "Kakao Name"),
                issuer(),
                userRepository);

        final var result = useCase.execute(new LoginCommand(ProviderType.KAKAO, "id-token"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(userRepository).save(any(User.class));
        final ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().providerSubject()).isEqualTo("kakao-subject");
        assertThat(captor.getValue().email()).isEqualTo("user@example.com");
        assertThat(captor.getValue().name()).isEqualTo("Kakao Name");
    }

    @Test
    void 기존_사용자는_프로필을_덮어쓰지_않는다() {
        final UserRepository userRepository = mock(UserRepository.class);
        final User user = user(1L);
        when(userRepository.findByProviderAndProviderSubject(any(), any())).thenReturn(Optional.of(user));
        final LoginUseCase useCase = new LoginUseCase(
                idToken -> new OAuthUserInfo("kakao-subject", "new@example.com", "New Name"),
                issuer(),
                userRepository);

        useCase.execute(new LoginCommand(ProviderType.KAKAO, "id-token"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 이름이_없는_신규_사용자는_기본_이름을_저장한다() {
        final UserRepository userRepository = mock(UserRepository.class);
        final User user = user(1L);
        when(userRepository.findByProviderAndProviderSubject(any(), any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        final LoginUseCase useCase = new LoginUseCase(
                idToken -> new OAuthUserInfo("kakao-subject", null, null),
                issuer(),
                userRepository);

        useCase.execute(new LoginCommand(ProviderType.KAKAO, "id-token"));

        final ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Kakao User");
        assertThat(captor.getValue().email()).isNull();
    }

    @Test
    void 빈_idToken은_Kakao_검증을_호출하지_않고_401을_던진다() {
        final LoginUseCase useCase = new LoginUseCase(
                idToken -> { throw new AssertionError("검증하면 안 됩니다."); },
                issuer(),
                mock(UserRepository.class));

        assertThatThrownBy(() -> useCase.execute(new LoginCommand(ProviderType.KAKAO, " ")))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.KAKAO_TOKEN_INVALID));
    }

    private AuthTokenIssuer issuer() {
        return new AuthTokenIssuer(
                new FixedTokenProvider(), new NoopRefreshTokenStore(), new RefreshTokenHasher(), Duration.ofDays(14));
    }

    private User user(final Long id) {
        final User user = mock(User.class);
        when(user.id()).thenReturn(id);
        when(user.role()).thenReturn(UserRole.USER);
        return user;
    }

    private static final class FixedTokenProvider implements TokenProvider {

        @Override
        public String createAccessToken(final Long userId, final UserRole role) {
            return "access-token";
        }

        @Override
        public String createRefreshToken(final Long userId) {
            return "refresh-token";
        }

        @Override
        public AccessTokenPayload parseAccessTokenPayload(final String token) {
            return new AccessTokenPayload(1L, UserRole.USER);
        }

        @Override
        public TokenPayload parseRefreshTokenPayload(final String token) {
            return new TokenPayload(1L, Instant.now().plusSeconds(60));
        }
    }

    private static final class NoopRefreshTokenStore implements RefreshTokenStore {

        @Override
        public void save(final Long userId, final String tokenHash, final Duration ttl) {}

        @Override
        public boolean matches(final Long userId, final String tokenHash) {
            return false;
        }

        @Override
        public void delete(final Long userId) {}
    }
}
