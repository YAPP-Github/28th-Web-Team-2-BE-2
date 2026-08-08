package com.example.demo.item.infrastructure.config;

import com.example.demo.item.infrastructure.crawler.SeleniumDriverFactory;
import com.example.demo.item.infrastructure.crawler.SeleniumProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SeleniumProperties.class)
public class CrawlerConfiguration {

    @Bean
    SeleniumDriverFactory seleniumDriverFactory(final SeleniumProperties properties) {
        return new SeleniumDriverFactory(properties);
    }
}
