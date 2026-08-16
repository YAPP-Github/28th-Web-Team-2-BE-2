package com.example.demo.external.kakao;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.springframework.http.HttpStatus;

public final class KakaoClientException extends ApiException {

    public KakaoClientException() {
        super(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY);
    }

    public KakaoClientException(final Exception cause) {
        super(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY,
                cause);
    }
}
