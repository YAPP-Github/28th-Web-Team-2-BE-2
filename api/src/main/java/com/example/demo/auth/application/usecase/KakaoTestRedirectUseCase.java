package com.example.demo.auth.application.usecase;

import com.example.demo.auth.application.port.KakaoTokenClient;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KakaoTestRedirectUseCase {

    private final KakaoTokenClient kakaoTokenClient;

    public String execute(final String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw invalidParameter();
        }
        return kakaoTokenClient.exchangeIdToken(authorizationCode);
    }

    private ApiException invalidParameter() {
        return new ApiException(
                ErrorType.INVALID_PARAMETER_ERROR.description(),
                ErrorType.INVALID_PARAMETER_ERROR,
                HttpStatus.BAD_REQUEST);
    }
}
