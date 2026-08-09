package com.example.demo.external.selenium;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

@RequiredArgsConstructor
public class SeleniumDriverFactory {

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

    ChromeOptions createOptions() {
        final ChromeOptions chromeOptions = new ChromeOptions();
        if (options.headless()) {
            chromeOptions.addArguments("--headless=new");
        }
        chromeOptions.addArguments("--disable-dev-shm-usage");
        return chromeOptions;
    }
}
