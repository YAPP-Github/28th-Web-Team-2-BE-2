package com.example.demo.auth.infrastructure.oauth;

import com.example.demo.auth.application.port.KakaoIdentityProvider;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KakaoIdTokenVerifierAdapter implements KakaoIdentityProvider {

    private final JwtDecoder jwtDecoder;

    public KakaoIdTokenVerifierAdapter(final JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public String verify(final String idToken) {
        try {
            final String subject = jwtDecoder.decode(idToken).getSubject();
            if (!StringUtils.hasText(subject)) {
                throw invalidToken();
            }
            return subject;
        } catch (final ApiException exception) {
            throw exception;
        } catch (final RuntimeException exception) {
            throw invalidToken();
        }
    }

    private ApiException invalidToken() {
        return new ApiException(
                ErrorType.KAKAO_TOKEN_INVALID.description(),
                ErrorType.KAKAO_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED);
    }
}
