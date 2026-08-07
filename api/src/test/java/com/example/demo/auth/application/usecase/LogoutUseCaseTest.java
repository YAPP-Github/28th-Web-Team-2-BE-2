package com.example.demo.auth.application.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.demo.auth.application.RefreshTokenHasher;
import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import org.junit.jupiter.api.Test;

class LogoutUseCaseTest {

    @Test
    void refresh_token이_있으면_Redis_키를_삭제한다() {
        final RefreshTokenStore store = mock(RefreshTokenStore.class);
        new LogoutUseCase(store, new RefreshTokenHasher()).execute(new LogoutCommand("refresh-token"));

        verify(store).delete(new RefreshTokenHasher().hash("refresh-token"));
    }

    @Test
    void refresh_token이_없어도_cookie_삭제를_막지_않는다() {
        final RefreshTokenStore store = mock(RefreshTokenStore.class);
        new LogoutUseCase(store, new RefreshTokenHasher()).execute(new LogoutCommand(null));

        verifyNoInteractions(store);
    }
}
