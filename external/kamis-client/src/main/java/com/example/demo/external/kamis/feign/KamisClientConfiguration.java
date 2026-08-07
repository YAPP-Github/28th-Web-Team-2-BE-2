package com.example.demo.external.kamis.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class KamisClientConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor(
            @Value("${kamis.cert-key}") final String certKey,
            @Value("${kamis.cert-id}") final String certId
    ) {
        return requestTemplate -> requestTemplate
                .query("p_cert_key", certKey)
                .query("p_cert_id", certId);
    }

    @Bean
    public KamisErrorDecoder kamisErrorDecoder(final ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new KamisErrorDecoder(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    public Decoder kamisResponseDecoder(final ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new KamisResponseDecoder(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }
}
