package com.example.demo.auth.infrastructure.token;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String TOKEN_TYPE_CLAIM = "type";

    private static final String ROLE_CLAIM = "role";

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.access-secret}") final String accessSecret,
            @Value("${jwt.refresh-secret}") final String refreshSecret,
            @Value("${jwt.access-token-expiration}") final Duration accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") final Duration refreshTokenExpiration) {
        this.accessSecretKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Override
    public String createAccessToken(final Long userId, final UserRole role) {
        return createToken(userId, ACCESS_TOKEN_TYPE, accessTokenExpiration, accessSecretKey, role);
    }

    @Override
    public String createRefreshToken(final Long userId) {
        return createToken(userId, REFRESH_TOKEN_TYPE, refreshTokenExpiration, refreshSecretKey, null);
    }

    @Override
    public AccessTokenPayload parseAccessTokenPayload(final String token) {
        final Claims claims = parseToken(token, accessSecretKey);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);
        try {
            final Long userId = Long.valueOf(claims.getSubject());
            final UserRole role = UserRole.valueOf(claims.get(ROLE_CLAIM, String.class));
            return new AccessTokenPayload(userId, role);
        } catch (final RuntimeException exception) {
            throw invalidToken();
        }
    }

    @Override
    public TokenPayload parseRefreshTokenPayload(final String token) {
        final Claims claims = parseToken(token, refreshSecretKey);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);
        try {
            return new TokenPayload(Long.valueOf(claims.getSubject()), claims.getExpiration().toInstant());
        } catch (final RuntimeException exception) {
            throw invalidToken();
        }
    }

    private String createToken(
            final Long userId,
            final String tokenType,
            final Duration expiration,
            final SecretKey secretKey,
            final UserRole role) {
        final Instant now = Instant.now();
        final var builder = Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey);
        if (role != null) {
            builder.claim(ROLE_CLAIM, role.name());
        }
        return builder.compact();
    }

    private Claims parseToken(final String token, final SecretKey secretKey) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (final JwtException | IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private void validateTokenType(final Claims claims, final String expectedTokenType) {
        if (!StringUtils.hasText(claims.getSubject())
                || claims.getExpiration() == null
                || !expectedTokenType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw invalidToken();
        }
    }

    private ApiException invalidToken() {
        return new ApiException(ErrorType.INVALID_TOKEN.description(), ErrorType.INVALID_TOKEN,
                org.springframework.http.HttpStatus.UNAUTHORIZED);
    }
}
