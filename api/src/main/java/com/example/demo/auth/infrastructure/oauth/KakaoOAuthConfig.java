package com.example.demo.auth.infrastructure.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

@Configuration
public class KakaoOAuthConfig {

    @Bean
    JwtDecoder kakaoJwtDecoder(
            @Value("${kakao.oauth.client-id}") final String clientId,
            @Value("${kakao.oauth.issuer}") final String issuer,
            @Value("${kakao.oauth.jwk-set-uri}") final String jwkSetUri) {
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        final OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator(clientId)));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(final String clientId) {
        return jwt -> {
            if (StringUtils.hasText(clientId) && jwt.getAudience().contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Kakao audience가 일치하지 않습니다.", null));
        };
    }
}
