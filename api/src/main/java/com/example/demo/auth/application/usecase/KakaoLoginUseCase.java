package com.example.demo.auth.application.usecase;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.command.KakaoLoginCommand;
import com.example.demo.auth.application.port.KakaoIdentityProvider;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KakaoLoginUseCase {

    private final KakaoIdentityProvider kakaoIdentityProvider;
    private final AuthTokenIssuer authTokenIssuer;

    public KakaoLoginUseCase(
            final KakaoIdentityProvider kakaoIdentityProvider, final AuthTokenIssuer authTokenIssuer) {
        this.kakaoIdentityProvider = kakaoIdentityProvider;
        this.authTokenIssuer = authTokenIssuer;
    }

    public AuthToken execute(final KakaoLoginCommand command) {
        if (!StringUtils.hasText(command.idToken())) {
            throw invalidKakaoToken();
        }
        return authTokenIssuer.issue(kakaoIdentityProvider.verify(command.idToken()));
    }

    private ApiException invalidKakaoToken() {
        return new ApiException(
                ErrorType.KAKAO_TOKEN_INVALID.description(),
                ErrorType.KAKAO_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED);
    }
}
