package com.example.demo.external.selenium;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

@RequiredArgsConstructor
public class SeleniumDriverFactory {

    private final SeleniumOptions options;

    public SeleniumPage loadPage(final URI targetUrl) {
        Objects.requireNonNull(targetUrl, "targetUrl must not be null");
        WebDriver driver = null;
        try {
            driver = create();
            driver.get(targetUrl.toString());
            final String pageSource = waitForPageSource(driver);
            return new SeleniumPage(currentUrl(driver, targetUrl), pageSource);
        } catch (WebDriverException exception) {
            throw new SeleniumPageLoadException(targetUrl, failureReason(exception), exception);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    WebDriver create() {
        final WebDriver driver = new ChromeDriver(createOptions());
        driver.manage().timeouts().pageLoadTimeout(options.pageLoadTimeout());
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        return driver;
    }

    WebDriverWait createWait(final WebDriver driver) {
        return new WebDriverWait(driver, options.waitTimeout());
    }

    private String waitForPageSource(final WebDriver driver) {
        return createWait(driver).until(currentDriver -> {
            final String pageSource = currentDriver.getPageSource();
            if (pageSource == null || pageSource.isBlank()) {
                return null;
            }
            return pageSource;
        });
    }

    private URI currentUrl(final WebDriver driver, final URI targetUrl) {
        final String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || currentUrl.isBlank()) {
            return targetUrl;
        }
        return URI.create(currentUrl);
    }

    private String failureReason(final WebDriverException exception) {
        final String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    ChromeOptions createOptions() {
        final ChromeOptions chromeOptions = new ChromeOptions();
        if (options.headless()) {
            chromeOptions.addArguments("--headless=new");
        }
        chromeOptions.addArguments("--disable-dev-shm-usage");
        return chromeOptions;
    }
}
