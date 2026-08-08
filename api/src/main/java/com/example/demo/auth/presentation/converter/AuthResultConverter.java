package com.example.demo.auth.presentation.converter;

import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.presentation.dto.TokenResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthResultConverter {

    public TokenResponse toTokenResponse(final AuthToken token) {
        return new TokenResponse(token.accessToken());
    }
}
