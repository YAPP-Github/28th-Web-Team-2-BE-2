package com.example.demo.external.kakao.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class KakaoMapClientConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor(
            @Value("${kakao.map.rest-api-key:}") final String apiKey) {
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
