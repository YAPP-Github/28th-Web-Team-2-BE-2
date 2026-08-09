package com.example.demo.external.selenium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

class SeleniumDriverFactoryTest {

    @Test
    void configuresHeadlessChromeOptions() {
        final SeleniumDriverFactory factory = new SeleniumDriverFactory(
                new SeleniumOptions(true, Duration.ofSeconds(30), Duration.ofSeconds(10)));

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
                new SeleniumOptions(false, Duration.ofSeconds(30), Duration.ofSeconds(10)));
        final WebDriver driver = mock(WebDriver.class);

        final WebDriverWait wait = factory.createWait(driver);

        assertThat(wait).isNotNull();
    }

    @Test
    void loadsPageAndClosesDriver() {
        final SeleniumDriverFactory factory = org.mockito.Mockito.spy(new SeleniumDriverFactory(
                new SeleniumOptions(false, Duration.ofSeconds(30), Duration.ofSeconds(10))));
        final WebDriver driver = mock(WebDriver.class);
        final WebDriverWait wait = mock(WebDriverWait.class);
        final URI targetUrl = URI.create("https://example.com/items");
        doReturn(driver).when(factory).create();
        doReturn(wait).when(factory).createWait(driver);
        when(driver.getCurrentUrl()).thenReturn(targetUrl.toString());
        doReturn("<html>items</html>").when(wait).until(org.mockito.Mockito.any());

        final SeleniumPage page = factory.loadPage(targetUrl);

        assertThat(page.sourceUrl()).isEqualTo(targetUrl);
        assertThat(page.html()).isEqualTo("<html>items</html>");
        verify(driver).get(targetUrl.toString());
        verify(driver).quit();
    }

    @Test
    void wrapsDriverCreationFailure() {
        final SeleniumDriverFactory factory = org.mockito.Mockito.spy(new SeleniumDriverFactory(
                new SeleniumOptions(false, Duration.ofSeconds(30), Duration.ofSeconds(10))));
        final URI targetUrl = URI.create("https://example.com/items");
        doThrow(new WebDriverException("driver unavailable"))
                .when(factory)
                .create();

        assertThatThrownBy(() -> factory.loadPage(targetUrl))
                .isInstanceOf(SeleniumPageLoadException.class)
                .hasMessageStartingWith("driver unavailable");
    }
}
