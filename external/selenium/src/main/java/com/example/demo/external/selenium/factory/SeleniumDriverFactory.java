package com.example.demo.external.selenium.factory;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.config.SeleniumOptions;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public class SeleniumDriverFactory {

    private static final String PAGE_LOAD_FAILURE_MESSAGE = "셀레니움 페이지 로딩에 실패했습니다.";

    private final SeleniumOptions options;

    public WebDriver create() {
        final WebDriver driver = new ChromeDriver(createOptions());
        driver.manage().timeouts().pageLoadTimeout(options.pageLoadTimeout());
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        return driver;
    }

    public WebDriverWait createWait(final WebDriver driver) {
        return new WebDriverWait(driver, options.waitTimeout());
    }

    public SeleniumPage loadPage(final URI targetUrl) {
        Objects.requireNonNull(targetUrl, "targetUrl must not be null");
        WebDriver driver = null;
        try {
            driver = create();
            driver.get(targetUrl.toString());
            createWait(driver).until(currentDriver -> !currentDriver.getPageSource().isBlank());
            return new SeleniumPage(URI.create(driver.getCurrentUrl()), driver.getPageSource());
        } catch (WebDriverException exception) {
            throw new ApiException(
                    PAGE_LOAD_FAILURE_MESSAGE,
                    ErrorType.EXTERNAL_API_ERROR,
                    HttpStatus.BAD_GATEWAY);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
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
