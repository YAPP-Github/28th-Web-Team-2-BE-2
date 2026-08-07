package com.example.demo.kamis.infrastructure;

import com.example.demo.kamis.application.port.KamisPriceQueryPort;
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
    KamisCredentials kamisCredentials(
            @Value("${kamis.cert-key}") final String certKey,
            @Value("${kamis.cert-id}") final String certId) {
        return new KamisCredentials(certKey, certId);
    }

    @Bean(name = "kamisPriceQueryPort")
    KamisPriceQueryPort kamisPriceQueryPort(
            final RestClient kamisRestClient, final KamisCredentials kamisCredentials) {
        return new KamisDailyPriceClient(kamisRestClient, kamisCredentials);
    }
}

record KamisCredentials(String certKey, String certId) {}
