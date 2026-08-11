package com.example.demo.item.infrastructure.config;

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

    private static final Duration DEFAULT_PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(10);

    public SeleniumProperties {
        if (pageLoadTimeout == null) {
            pageLoadTimeout = DEFAULT_PAGE_LOAD_TIMEOUT;
        }
        if (waitTimeout == null) {
            waitTimeout = DEFAULT_WAIT_TIMEOUT;
        }
        validateNonNegative(pageLoadTimeout);
        validateNonNegative(waitTimeout);
    }

    private static void validateNonNegative(final Duration timeout) {
        if (timeout.isNegative()) {
            throw new ApiException(
                    ErrorType.CONFIGURATION_ERROR.description(),
                    ErrorType.CONFIGURATION_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
