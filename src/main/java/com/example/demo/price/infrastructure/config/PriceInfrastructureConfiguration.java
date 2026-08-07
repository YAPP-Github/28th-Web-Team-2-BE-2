package com.example.demo.price.infrastructure.config;

import com.example.demo.price.domain.matching.ProductMatcher;
import com.example.demo.price.domain.normalization.PriceNormalizer;
import com.example.demo.price.domain.normalization.QuantityParser;
import com.example.demo.price.domain.normalization.RepresentativePriceCalculator;
import com.example.demo.price.infrastructure.crawler.SeleniumProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SeleniumProperties.class)
public class PriceInfrastructureConfiguration {

    @Bean
    ProductMatcher productMatcher() {
        return new ProductMatcher();
    }

    @Bean
    QuantityParser quantityParser() {
        return new QuantityParser();
    }

    @Bean
    PriceNormalizer priceNormalizer() {
        return new PriceNormalizer();
    }

    @Bean
    RepresentativePriceCalculator representativePriceCalculator() {
        return new RepresentativePriceCalculator();
    }
}
