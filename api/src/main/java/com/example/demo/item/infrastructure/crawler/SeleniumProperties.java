package com.example.demo.item.infrastructure.crawler;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "item.price.collection.selenium")
public record SeleniumProperties(
        boolean headless,
        Duration pageLoadTimeout,
        Duration waitTimeout) {

    public SeleniumProperties {
        if (pageLoadTimeout == null) {
            pageLoadTimeout = Duration.ofSeconds(30);
        }
        if (waitTimeout == null) {
            waitTimeout = Duration.ofSeconds(10);
        }
        validateNonNegative("pageLoadTimeout", pageLoadTimeout);
        validateNonNegative("waitTimeout", waitTimeout);
    }

    private static void validateNonNegative(final String propertyName, final Duration timeout) {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must not be negative");
        }
    }
}
