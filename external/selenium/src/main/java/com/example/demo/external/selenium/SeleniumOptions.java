package com.example.demo.external.selenium;

import java.time.Duration;

public record SeleniumOptions(
        boolean headless,
        Duration pageLoadTimeout,
        Duration waitTimeout) {
}
