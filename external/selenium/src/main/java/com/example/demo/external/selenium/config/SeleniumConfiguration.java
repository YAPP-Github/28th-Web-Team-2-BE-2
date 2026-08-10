package com.example.demo.external.selenium.config;

import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SeleniumProperties.class)
public class SeleniumConfiguration {

    @Bean
    SeleniumDriverFactory seleniumDriverFactory(final SeleniumProperties properties) {
        return new SeleniumDriverFactory(new SeleniumOptions(
                properties.headless(),
                properties.pageLoadTimeout(),
                properties.waitTimeout()));
    }
}
