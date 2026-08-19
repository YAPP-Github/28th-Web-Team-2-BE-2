package com.example.demo.item.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.demo.item.application.usecase.CollectOnlinePriceUseCase;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class OnlinePriceSchedulingConfigTest {

    private static final String PROPERTY = "item.price.collection.scheduler.enabled";

    @Test
    void scheduler는_property가_true일_때만_생성된다() {
        try (AnnotationConfigApplicationContext enabled = context("true")) {
            assertThat(enabled.getBeansOfType(OnlinePriceScheduler.class)).hasSize(1);
            final ThreadPoolTaskExecutor executor = enabled.getBean(ThreadPoolTaskExecutor.class);
            assertThat(executor.getCorePoolSize()).isEqualTo(1);
            assertThat(executor.getMaxPoolSize()).isEqualTo(1);
            assertThat(executor.getQueueCapacity()).isZero();
        }
        try (AnnotationConfigApplicationContext disabled = context("false")) {
            assertThat(disabled.getBeansOfType(OnlinePriceScheduler.class)).isEmpty();
        }
        try (AnnotationConfigApplicationContext missing = context(null)) {
            assertThat(missing.getBeansOfType(OnlinePriceScheduler.class)).isEmpty();
        }
    }

    @Test
    void config은_명시적_true와_기본_false를_사용하고_스케줄링을_중복_활성화하지_않는다() throws Exception {
        final Method beanMethod = OnlinePriceSchedulingConfig.class.getDeclaredMethod(
                "onlinePriceScheduler", CollectOnlinePriceUseCase.class, TaskExecutor.class);
        final ConditionalOnProperty condition = beanMethod.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition.name()).containsExactly(PROPERTY);
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isFalse();
        assertThat(OnlinePriceSchedulingConfig.class.isAnnotationPresent(EnableScheduling.class))
                .isFalse();
    }

    private AnnotationConfigApplicationContext context(final String enabled) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (enabled != null) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of(PROPERTY, enabled)));
        }
        context.registerBean(CollectOnlinePriceUseCase.class, () -> mock(CollectOnlinePriceUseCase.class));
        context.register(OnlinePriceSchedulingConfig.class);
        context.refresh();
        return context;
    }
}
