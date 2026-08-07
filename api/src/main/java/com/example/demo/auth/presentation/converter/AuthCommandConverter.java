package com.example.demo.auth.presentation.converter;

import com.example.demo.auth.application.command.KakaoLoginCommand;
import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.presentation.dto.KakaoLoginRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthCommandConverter {

    public KakaoLoginCommand toKakaoLoginCommand(final KakaoLoginRequest request) {
        return new KakaoLoginCommand(request.idToken());
    }

    public RefreshTokenCommand toRefreshTokenCommand(final String refreshToken) {
        return new RefreshTokenCommand(refreshToken);
    }

    public LogoutCommand toLogoutCommand(final String refreshToken) {
        return new LogoutCommand(refreshToken);
    }
}
