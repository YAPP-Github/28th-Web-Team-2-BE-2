package com.example.demo.external.kamis;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class KamisClientConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor(
            @Value("${kamis.cert-key}") final String certKey,
            @Value("${kamis.cert-id}") final String certId) {
        return requestTemplate -> requestTemplate
                .query("p_cert_key", certKey)
                .query("p_cert_id", certId);
    }
}
