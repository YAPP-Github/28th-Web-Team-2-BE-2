package com.example.demo.item.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SeleniumConfiguration.class)
@TestPropertySource(properties = {
        "item.price.collection.selenium.headless=true",
        "item.price.collection.selenium.page-load-timeout=45s",
        "item.price.collection.selenium.wait-timeout=12s"
})
class SeleniumPropertiesBindingTest {

    @Autowired
    private SeleniumProperties seleniumProperties;

    @Autowired
    private SeleniumDriverFactory seleniumDriverFactory;

    @Test
    void bindsItemPriceSeleniumProperties() {
        assertThat(seleniumProperties.headless()).isTrue();
        assertThat(seleniumProperties.pageLoadTimeout()).hasSeconds(45);
        assertThat(seleniumProperties.waitTimeout()).hasSeconds(12);
        assertThat(seleniumDriverFactory).isNotNull();
    }

    @Test
    void appliesTimeoutDefaults() {
        final SeleniumProperties properties = new SeleniumProperties(false, null, null);

        assertThat(properties.pageLoadTimeout()).hasSeconds(30);
        assertThat(properties.waitTimeout()).hasSeconds(10);
    }

    @Test
    void rejectsNegativePageLoadTimeout() {
        assertThatThrownBy(() -> new SeleniumProperties(
                false, Duration.ofSeconds(-1), Duration.ofSeconds(10)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.CONFIGURATION_ERROR);
                    assertThat(exception.errorMessage())
                            .isEqualTo(ErrorType.CONFIGURATION_ERROR.description());
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    @Test
    void rejectsNegativeWaitTimeout() {
        assertThatThrownBy(() -> new SeleniumProperties(
                false, Duration.ofSeconds(30), Duration.ofSeconds(-1)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.CONFIGURATION_ERROR);
                    assertThat(exception.errorMessage())
                            .isEqualTo(ErrorType.CONFIGURATION_ERROR.description());
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }
}
