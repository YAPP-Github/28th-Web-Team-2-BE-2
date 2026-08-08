package com.example.demo.item.infrastructure.crawler;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

@RequiredArgsConstructor
public class SeleniumDriverFactory {

    private final SeleniumProperties properties;

    public WebDriver create() {
        final WebDriver driver = new ChromeDriver(createOptions());
        driver.manage().timeouts().pageLoadTimeout(properties.pageLoadTimeout());
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        return driver;
    }

    public WebDriverWait createWait(final WebDriver driver) {
        return new WebDriverWait(driver, properties.waitTimeout());
    }

    ChromeOptions createOptions() {
        final ChromeOptions options = new ChromeOptions();
        if (properties.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-dev-shm-usage");
        return options;
    }
}
