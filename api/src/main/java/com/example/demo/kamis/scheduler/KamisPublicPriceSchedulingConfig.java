package com.example.demo.kamis.scheduler;

import com.example.demo.kamis.application.usecase.CollectKamisPublicPriceUseCase;
import com.example.demo.kamis.application.usecase.KamisHistoricalPublicPriceBackfillUseCase;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
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

    @Bean
    @ConditionalOnProperty(name = "kamis.public-price.backfill.enabled", havingValue = "true")
    KamisPublicPriceBackfillRunner kamisPublicPriceBackfillRunner(
            final KamisHistoricalPublicPriceBackfillUseCase useCase,
            @Value("${kamis.public-price.backfill.region-ids:}") final String regionIds,
            @Value("${kamis.public-price.backfill.region-id:}") final String legacyRegionId,
            @Value("${kamis.public-price.backfill.country-code:1101}") final String countryCode) {
        return new KamisPublicPriceBackfillRunner(
                useCase,
                validateRegionIds(firstNonBlank(regionIds, legacyRegionId), "backfill"),
                validateCountryCode(countryCode, "backfill"),
                Clock.system(ZoneId.of("Asia/Seoul")));
    }

    private String validateRegionId(final String regionId) {
        return validateRegionId(regionId, "collection");
    }

    private String validateRegionId(final String regionId, final String propertyGroup) {
        if (regionId == null || !regionId.matches(REGION_ID_PATTERN)) {
            throw new IllegalStateException(
                    "kamis.public-price." + propertyGroup + ".region-id must be a 10-digit region ID");
        }
        return regionId;
    }

    private List<String> validateRegionIds(final String regionIds, final String propertyGroup) {
        if (regionIds == null) {
            throw invalidRegionIds(propertyGroup);
        }
        final List<String> parsedRegionIds = Arrays.stream(regionIds.split(",", -1))
                .map(String::trim)
                .toList();
        if (parsedRegionIds.stream().anyMatch(regionId -> !regionId.matches(REGION_ID_PATTERN))) {
            throw invalidRegionIds(propertyGroup);
        }
        return parsedRegionIds;
    }

    private IllegalStateException invalidRegionIds(final String propertyGroup) {
        return new IllegalStateException(
                "kamis.public-price." + propertyGroup + ".region-ids must be comma-separated 10-digit region IDs");
    }

    private String firstNonBlank(final String preferred, final String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private String validateCountryCode(final String countryCode) {
        return validateCountryCode(countryCode, "collection");
    }

    private String validateCountryCode(final String countryCode, final String propertyGroup) {
        if (countryCode == null || !countryCode.matches(COUNTRY_CODE_PATTERN)) {
            throw new IllegalStateException(
                    "kamis.public-price." + propertyGroup + ".country-code must be a 4-digit code");
        }
        return countryCode;
    }
}
