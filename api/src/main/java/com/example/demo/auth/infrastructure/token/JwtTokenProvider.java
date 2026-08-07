package com.example.demo.auth.infrastructure.token;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.TokenPayload;
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

    private final SecretKey secretKey;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret-key}") final String secret,
            @Value("${jwt.access-token-expiration}") final Duration accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") final Duration refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Override
    public String createAccessToken(final String subject) {
        return createToken(subject, ACCESS_TOKEN_TYPE, accessTokenExpiration);
    }

    @Override
    public String createRefreshToken(final String subject) {
        return createToken(subject, REFRESH_TOKEN_TYPE, refreshTokenExpiration);
    }

    @Override
    public TokenPayload parseAccessTokenPayload(final String token) {
        return parseTokenPayload(token, ACCESS_TOKEN_TYPE);
    }

    @Override
    public TokenPayload parseRefreshTokenPayload(final String token) {
        return parseTokenPayload(token, REFRESH_TOKEN_TYPE);
    }

    private String createToken(final String subject, final String tokenType, final Duration expiration) {
        final Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey)
                .compact();
    }

    private TokenPayload parseTokenPayload(final String token, final String expectedTokenType) {
        try {
            final Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            final String subject = claims.getSubject();
            final Date expiration = claims.getExpiration();
            if (!StringUtils.hasText(subject)
                    || expiration == null
                    || !expectedTokenType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                throw invalidToken();
            }
            return new TokenPayload(subject, expiration.toInstant());
        } catch (final ApiException exception) {
            throw exception;
        } catch (final JwtException | IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private ApiException invalidToken() {
        return new ApiException(ErrorType.INVALID_TOKEN.description(), ErrorType.INVALID_TOKEN,
                org.springframework.http.HttpStatus.UNAUTHORIZED);
    }
}
