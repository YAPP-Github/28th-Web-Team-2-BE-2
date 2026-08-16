package com.example.demo.external.kakao.feign;

import com.example.demo.external.kakao.KakaoClientException;
import feign.Response;
import feign.codec.ErrorDecoder;

public final class KakaoErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return new KakaoClientException(
                new IllegalStateException("Kakao request failed: " + response.status()));
    }
}
