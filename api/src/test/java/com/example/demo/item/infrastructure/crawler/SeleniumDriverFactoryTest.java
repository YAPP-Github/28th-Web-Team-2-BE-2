package com.example.demo.item.infrastructure.crawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

class SeleniumDriverFactoryTest {

    @Test
    void configuresHeadlessChromeOptions() {
        final SeleniumDriverFactory factory = new SeleniumDriverFactory(
                new SeleniumProperties(true, Duration.ofSeconds(30), Duration.ofSeconds(10)));

        final ChromeOptions options = factory.createOptions();

        final Map<?, ?> chromeOptions = (Map<?, ?>) options.asMap().get("goog:chromeOptions");
        @SuppressWarnings("unchecked")
        final List<String> arguments = (List<String>) chromeOptions.get("args");

        assertThat(arguments).contains("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
    }
}
