package com.example.demo.item.infrastructure.crawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.example.demo.item.infrastructure.config.CrawlerConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = CrawlerConfiguration.class)
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
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SeleniumProperties(
                        false, Duration.ofSeconds(-1), Duration.ofSeconds(10)));
    }

    @Test
    void rejectsNegativeWaitTimeout() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SeleniumProperties(
                        false, Duration.ofSeconds(30), Duration.ofSeconds(-1)));
    }
}
