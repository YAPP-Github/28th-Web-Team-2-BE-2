package com.example.demo.common.config.kakao;

import com.example.demo.external.kakao.KakaoLocalClient;
import com.example.demo.external.kakao.KakaoLocalRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KakaoLocalClientConfiguration {

    @Bean
    KakaoLocalClient kakaoLocalClient(
            @Value("${kakao.local.base-url:https://dapi.kakao.com}") final String baseUrl,
            @Value("${kakao.local.rest-api-key:}") final String apiKey) {
        return new KakaoLocalRestClient(baseUrl, apiKey);
    }
}
