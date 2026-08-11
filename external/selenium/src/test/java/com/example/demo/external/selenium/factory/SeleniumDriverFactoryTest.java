package com.example.demo.external.selenium.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.config.SeleniumOptions;
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
    void loadsPageAndReturnsRedirectedUrlAndSource() {
        final SeleniumDriverFactory factory = spy(new SeleniumDriverFactory(
                new SeleniumOptions(true, Duration.ofSeconds(30), Duration.ofSeconds(10))));
        final WebDriver driver = mock(WebDriver.class);
        final WebDriverWait wait = mock(WebDriverWait.class);
        final URI requestedUrl = URI.create("https://example.com/items");
        final URI redirectedUrl = URI.create("https://example.com/items?loaded=true");
        doReturn(driver).when(factory).create();
        doReturn(wait).when(factory).createWait(driver);
        doReturn(true).when(wait).until(any());
        when(driver.getCurrentUrl()).thenReturn(redirectedUrl.toString());
        when(driver.getPageSource()).thenReturn("<html>items</html>");

        final SeleniumPage page = factory.loadPage(requestedUrl);

        assertThat(page.sourceUrl()).isEqualTo(redirectedUrl);
        assertThat(page.html()).isEqualTo("<html>items</html>");
        verify(driver).get(requestedUrl.toString());
        verify(driver).quit();
    }

    @Test
    void convertsSeleniumFailureToCommonApiExceptionAndQuitsDriver() {
        final SeleniumDriverFactory factory = spy(new SeleniumDriverFactory(
                new SeleniumOptions(true, Duration.ofSeconds(30), Duration.ofSeconds(10))));
        final WebDriver driver = mock(WebDriver.class);
        doReturn(driver).when(factory).create();
        doThrow(new WebDriverException("connection refused"))
                .when(driver).get("https://example.com/items");

        assertThatThrownBy(() -> factory.loadPage(URI.create("https://example.com/items")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR));

        verify(driver).quit();
    }
}
