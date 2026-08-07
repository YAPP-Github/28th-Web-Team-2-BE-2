package com.example.demo.external.kamis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class KamisClientConfiguration {

    @Bean
    RestClient kamisRestClient(@Value("${kamis.url}") final String url) {
        return RestClient.builder().baseUrl(url).build();
    }

    @Bean
    KamisClient kamisClient(
            final RestClient kamisRestClient,
            @Value("${kamis.cert-key}") final String certKey,
            @Value("${kamis.cert-id}") final String certId) {
        return new DefaultKamisClient(kamisRestClient, new KamisCredentials(certKey, certId));
    }
}

record KamisCredentials(String certKey, String certId) {}
