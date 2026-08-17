package com.example.demo.news.scheduler;

import com.example.demo.news.application.usecase.CollectNewsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
class NewsSchedulingConfig {

    @Bean
    @ConditionalOnProperty(name = "news.crawler.enabled", havingValue = "true")
    NewsCrawlerScheduler newsCrawlerScheduler(final CollectNewsUseCase collectNewsUseCase) {
        return new NewsCrawlerScheduler(collectNewsUseCase);
    }
}
