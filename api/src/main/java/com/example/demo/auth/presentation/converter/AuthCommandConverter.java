package com.example.demo.auth.presentation.converter;

import com.example.demo.auth.application.command.KakaoLoginCommand;
import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.presentation.dto.KakaoLoginRequest;
import com.example.demo.auth.presentation.dto.RefreshTokenRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthCommandConverter {

    public KakaoLoginCommand toKakaoLoginCommand(final KakaoLoginRequest request) {
        return new KakaoLoginCommand(request.idToken());
    }

    public RefreshTokenCommand toRefreshTokenCommand(final RefreshTokenRequest request) {
        return new RefreshTokenCommand(request.refreshToken());
    }

    public LogoutCommand toLogoutCommand(final Long userId) {
        return new LogoutCommand(userId);
    }
}
