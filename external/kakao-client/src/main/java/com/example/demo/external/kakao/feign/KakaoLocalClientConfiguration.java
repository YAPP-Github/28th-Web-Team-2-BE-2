package com.example.demo.external.kakao.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class KakaoLocalClientConfiguration {

    @Bean
    public RequestInterceptor kakaoRequestInterceptor(
            @Value("${kakao.local.rest-api-key:}") final String apiKey) {
        return requestTemplate -> requestTemplate.header("Authorization", "KakaoAK " + apiKey);
    }

    @Bean
    public Decoder kakaoResponseDecoder(final ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new KakaoResponseDecoder(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    public ErrorDecoder kakaoErrorDecoder() {
        return new KakaoErrorDecoder();
    }
}
