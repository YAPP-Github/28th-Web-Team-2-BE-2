package com.example.demo.auth.application.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import org.junit.jupiter.api.Test;

class LogoutUseCaseTest {

    @Test
    void 인증된_사용자의_Redis_Refresh_Session을_삭제한다() {
        final RefreshTokenStore store = mock(RefreshTokenStore.class);

        new LogoutUseCase(store).execute(new LogoutCommand(1L));

        verify(store).delete(1L);
    }
}
