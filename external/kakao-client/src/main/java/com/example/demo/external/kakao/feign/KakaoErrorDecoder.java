package com.example.demo.external.kakao.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.util.Optional;
import org.springframework.http.HttpStatus;

public final class KakaoErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(final String methodKey, final Response response) {
        final HttpStatus httpStatus = Optional.ofNullable(HttpStatus.resolve(response.status()))
                .orElse(HttpStatus.BAD_GATEWAY);
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                httpStatus);
    }
}
