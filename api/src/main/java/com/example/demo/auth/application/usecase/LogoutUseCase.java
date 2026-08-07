package com.example.demo.auth.application.usecase;

import com.example.demo.auth.application.RefreshTokenHasher;
import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.port.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenHasher refreshTokenHasher;

    public void execute(final LogoutCommand command) {
        if (StringUtils.hasText(command.refreshToken())) {
            refreshTokenStore.delete(refreshTokenHasher.hash(command.refreshToken()));
        }
    }
}
