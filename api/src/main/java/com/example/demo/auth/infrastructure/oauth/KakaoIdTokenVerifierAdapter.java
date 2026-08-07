package com.example.demo.auth.infrastructure.oauth;

import com.example.demo.auth.application.port.OAuthIdentityVerifier;
import com.example.demo.auth.application.result.OAuthUserInfo;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KakaoIdTokenVerifierAdapter implements OAuthIdentityVerifier {

    private final JwtDecoder jwtDecoder;

    public KakaoIdTokenVerifierAdapter(final JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public OAuthUserInfo verify(final String idToken) {
        try {
            final var jwt = jwtDecoder.decode(idToken);
            final String subject = jwt.getSubject();
            if (!StringUtils.hasText(subject)) {
                throw invalidToken();
            }
            return new OAuthUserInfo(
                    subject,
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("nickname"));
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
