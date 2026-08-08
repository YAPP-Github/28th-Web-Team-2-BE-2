package com.example.demo.item.infrastructure.crawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

class SeleniumDriverFactoryTest {

    @Test
    void configuresHeadlessChromeOptions() {
        final SeleniumDriverFactory factory = new SeleniumDriverFactory(
                new SeleniumProperties(true, Duration.ofSeconds(30), Duration.ofSeconds(10)));

        final ChromeOptions options = factory.createOptions();

        final Map<?, ?> chromeOptions = (Map<?, ?>) options.asMap().get("goog:chromeOptions");
        @SuppressWarnings("unchecked")
        final List<String> arguments = (List<String>) chromeOptions.get("args");

        assertThat(arguments).contains("--headless=new", "--disable-dev-shm-usage");
        assertThat(arguments).doesNotContain("--no-sandbox");
    }

    @Test
    void createsExplicitWait() {
        final SeleniumDriverFactory factory = new SeleniumDriverFactory(
                new SeleniumProperties(false, Duration.ofSeconds(30), Duration.ofSeconds(10)));
        final WebDriver driver = Mockito.mock(WebDriver.class);

        final WebDriverWait wait = factory.createWait(driver);

        assertThat(wait).isNotNull();
    }
}
