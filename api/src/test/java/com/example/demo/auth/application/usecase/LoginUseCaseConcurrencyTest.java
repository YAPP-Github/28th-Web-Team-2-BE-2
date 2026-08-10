package com.example.demo.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.command.LoginCommand;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.result.OAuthUserInfo;
import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.persistence.UserRepositoryAdapter;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LoginUseCaseConcurrencyTest {

    private final UserJpaRepository userJpaRepository;
    private final UserRepositoryAdapter userRepositoryAdapter;

    @Autowired
    LoginUseCaseConcurrencyTest(
            final UserJpaRepository userJpaRepository,
            final UserRepositoryAdapter userRepositoryAdapter) {
        this.userJpaRepository = userJpaRepository;
        this.userRepositoryAdapter = userRepositoryAdapter;
    }

    @BeforeEach
    void setUp() {
        userJpaRepository.deleteAll();
    }

    @Test
    void 동일한_OAuth_사용자의_동시_최초_로그인은_한_명의_사용자로_성공한다() throws Exception {
        final CyclicBarrier verifierBarrier = new CyclicBarrier(2);
        final AuthTokenIssuer authTokenIssuer = mock(AuthTokenIssuer.class);
        when(authTokenIssuer.issue(anyLong(), eq(UserRole.USER)))
                .thenReturn(new AuthToken("access-token", "refresh-token"));
        final LoginUseCase useCase = new LoginUseCase(
                idToken -> {
                    await(verifierBarrier);
                    return new OAuthUserInfo("same-subject", "user@example.com", "Kakao User");
                },
                authTokenIssuer,
                userRepositoryAdapter);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            final List<Future<AuthToken>> results = List.of(
                    executor.submit(() -> useCase.execute(new LoginCommand(ProviderType.KAKAO, "id-token"))),
                    executor.submit(() -> useCase.execute(new LoginCommand(ProviderType.KAKAO, "id-token"))));

            assertThat(results)
                    .allSatisfy(result -> assertThat(result.get(10, TimeUnit.SECONDS).accessToken())
                            .isEqualTo("access-token"));
        }

        assertThat(userJpaRepository.count()).isEqualTo(1L);
    }

    private void await(final CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (final Exception exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 로그인 테스트 동기화에 실패했습니다.", exception);
        }
    }
}
