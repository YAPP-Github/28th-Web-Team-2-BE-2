package com.example.demo.item.infrastructure.crawler;

import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class SeleniumDriverFactory {

    private final SeleniumProperties properties;

    public SeleniumDriverFactory(final SeleniumProperties properties) {
        this.properties = properties;
    }

    public WebDriver create() {
        final WebDriver driver = new ChromeDriver(createOptions());
        driver.manage().timeouts().pageLoadTimeout(properties.pageLoadTimeout());
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        return driver;
    }

    ChromeOptions createOptions() {
        final ChromeOptions options = new ChromeOptions();
        if (properties.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        return options;
    }
}
