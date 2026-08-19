package com.example.demo.item.scheduler;

import com.example.demo.item.application.usecase.CollectOnlinePriceUseCase;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
class OnlinePriceSchedulingConfig {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(
            name = "item.price.collection.scheduler.enabled",
            havingValue = "true",
            matchIfMissing = false)
    ThreadPoolTaskExecutor onlinePriceSchedulerExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("online-price-scheduler-");
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnProperty(
            name = "item.price.collection.scheduler.enabled",
            havingValue = "true",
            matchIfMissing = false)
    OnlinePriceScheduler onlinePriceScheduler(
            final CollectOnlinePriceUseCase useCase,
            @Qualifier("onlinePriceSchedulerExecutor") final TaskExecutor executor) {
        return new OnlinePriceScheduler(useCase, Clock.system(SEOUL), executor);
    }
}
