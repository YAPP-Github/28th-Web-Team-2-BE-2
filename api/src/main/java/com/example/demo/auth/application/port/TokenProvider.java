package com.example.demo.auth.application.port;

import com.example.demo.auth.application.result.TokenPayload;

public interface TokenProvider {

    String createAccessToken(String subject);

    String createRefreshToken(String subject);

    TokenPayload parseAccessTokenPayload(String token);

    TokenPayload parseRefreshTokenPayload(String token);
}
