package com.example.demo.external.kakao.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

public class KakaoMapClientConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor(
            @Value("${kakao.map.rest-api-key}") final String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(
                    ErrorType.CONFIGURATION_ERROR.description(),
                    ErrorType.CONFIGURATION_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return requestTemplate -> requestTemplate.header("Authorization", "KakaoAK " + apiKey);
    }

    @Bean
    public ErrorDecoder kakaoErrorDecoder() {
        return new KakaoErrorDecoder();
    }

    @Bean
    public Decoder kakaoResponseDecoder(final ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new KakaoResponseDecoder(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }
}
