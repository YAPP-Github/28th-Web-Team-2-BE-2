package com.example.demo.auth.presentation.converter;

import com.example.demo.auth.application.command.LoginCommand;
import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.presentation.dto.LoginRequest;
import com.example.demo.auth.presentation.dto.RefreshTokenRequest;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthCommandConverter {

    public LoginCommand toLoginCommand(final String providerType, final LoginRequest request) {
        return new LoginCommand(toProviderType(providerType), request.idToken());
    }

    public RefreshTokenCommand toRefreshTokenCommand(final RefreshTokenRequest request) {
        return new RefreshTokenCommand(request.refreshToken());
    }

    public LogoutCommand toLogoutCommand(final Long userId) {
        return new LogoutCommand(userId);
    }

    private ProviderType toProviderType(final String providerType) {
        if (!StringUtils.hasText(providerType)) {
            throw unsupportedProvider();
        }
        try {
            return ProviderType.valueOf(providerType.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw unsupportedProvider();
        }
    }

    private ApiException unsupportedProvider() {
        return new ApiException(
                ErrorType.INVALID_PARAMETER_ERROR.description(),
                ErrorType.INVALID_PARAMETER_ERROR,
                HttpStatus.BAD_REQUEST);
    }
}
