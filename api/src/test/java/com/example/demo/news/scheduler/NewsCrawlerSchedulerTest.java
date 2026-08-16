package com.example.demo.news.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.demo.news.application.usecase.CollectNewsUseCase;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

class NewsCrawlerSchedulerTest {

    @Test
    void startup과_스케줄_실행은_수집을_호출한다() {
        final CollectNewsUseCase collectNewsUseCase = mock(CollectNewsUseCase.class);
        final NewsCrawlerScheduler scheduler = new NewsCrawlerScheduler(collectNewsUseCase);

        scheduler.collectOnReady();
        scheduler.collectBySchedule();

        verify(collectNewsUseCase, times(2)).execute();
    }

    @Test
    void 수집_실패는_스케줄러를_중단시키지_않는다() {
        final CollectNewsUseCase collectNewsUseCase = mock(CollectNewsUseCase.class);
        doThrow(new IllegalStateException("crawl failed")).when(collectNewsUseCase).execute();
        final NewsCrawlerScheduler scheduler = new NewsCrawlerScheduler(collectNewsUseCase);

        assertThatCode(scheduler::collectOnReady).doesNotThrowAnyException();
        assertThatCode(scheduler::collectBySchedule).doesNotThrowAnyException();
    }

    @Test
    void scheduling_config은_scheduler_bean을_생성한다() throws Exception {
        final CollectNewsUseCase collectNewsUseCase = mock(CollectNewsUseCase.class);

        final NewsCrawlerScheduler scheduler = new NewsSchedulingConfig()
                .newsCrawlerScheduler(collectNewsUseCase);

        org.assertj.core.api.Assertions.assertThat(scheduler).isNotNull();

        final Method scheduledMethod = NewsCrawlerScheduler.class.getDeclaredMethod("collectBySchedule");
        final Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        org.assertj.core.api.Assertions.assertThat(scheduled.cron()).isEqualTo("0 0 */3 * * *");
        org.assertj.core.api.Assertions.assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");

        final Method readyMethod = NewsCrawlerScheduler.class.getDeclaredMethod("collectOnReady");
        org.assertj.core.api.Assertions.assertThat(readyMethod.getAnnotation(EventListener.class).value())
                .containsExactly(ApplicationReadyEvent.class);

        final Method beanMethod = NewsSchedulingConfig.class.getDeclaredMethod(
                "newsCrawlerScheduler", CollectNewsUseCase.class);
        final ConditionalOnProperty condition = beanMethod.getAnnotation(ConditionalOnProperty.class);
        org.assertj.core.api.Assertions.assertThat(condition.name()).containsExactly("news.crawler.enabled");
        org.assertj.core.api.Assertions.assertThat(condition.havingValue()).isEqualTo("true");
    }
}
