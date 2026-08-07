package com.example.demo.auth.application.usecase;

import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenStore refreshTokenStore;

    public void execute(final LogoutCommand command) {
        refreshTokenStore.delete(command.userId());
    }
}
