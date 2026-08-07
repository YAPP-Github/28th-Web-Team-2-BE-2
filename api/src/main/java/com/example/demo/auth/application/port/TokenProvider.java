package com.example.demo.auth.application.port;

import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.auth.domain.UserRole;

public interface TokenProvider {

    String createAccessToken(Long userId, UserRole role);

    String createRefreshToken(Long userId);

    AccessTokenPayload parseAccessTokenPayload(String token);

    TokenPayload parseRefreshTokenPayload(String token);
}
