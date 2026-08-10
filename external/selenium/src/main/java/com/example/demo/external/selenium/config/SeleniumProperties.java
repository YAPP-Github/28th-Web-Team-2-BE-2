package com.example.demo.external.selenium.config;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;

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
            throw new ApiException(
                    ErrorType.CONFIGURATION_ERROR.description(),
                    ErrorType.CONFIGURATION_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
