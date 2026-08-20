package com.example.demo.kamis.scheduler;

import com.example.demo.kamis.application.usecase.CollectKamisPublicPriceUseCase;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class KamisPublicPriceSchedulingConfig {

    private static final String REGION_ID_PATTERN = "\\d{10}";
    private static final String COUNTRY_CODE_PATTERN = "\\d{4}";

    @Bean
    @ConditionalOnProperty(name = "kamis.public-price.collection.enabled", havingValue = "true")
    KamisPublicPriceScheduler kamisPublicPriceScheduler(
            final CollectKamisPublicPriceUseCase useCase,
            @Value("${kamis.public-price.collection.region-id}") final String regionId,
            @Value("${kamis.public-price.collection.country-code:1101}") final String countryCode) {
        return new KamisPublicPriceScheduler(useCase, validateRegionId(regionId), validateCountryCode(countryCode),
                Clock.system(ZoneId.of("Asia/Seoul")));
    }

    private String validateRegionId(final String regionId) {
        if (regionId == null || !regionId.matches(REGION_ID_PATTERN)) {
            throw new IllegalStateException("kamis.public-price.collection.region-id must be a 10-digit region ID");
        }
        return regionId;
    }

    private String validateCountryCode(final String countryCode) {
        if (countryCode == null || !countryCode.matches(COUNTRY_CODE_PATTERN)) {
            throw new IllegalStateException("kamis.public-price.collection.country-code must be a 4-digit code");
        }
        return countryCode;
    }
}
