package com.example.demo.price.infrastructure.crawler;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "price.collection.selenium")
public record SeleniumProperties(
        boolean headless,
        Duration pageLoadTimeout) {

    public SeleniumProperties {
        pageLoadTimeout = pageLoadTimeout == null ? Duration.ofSeconds(30) : pageLoadTimeout;
    }
}
