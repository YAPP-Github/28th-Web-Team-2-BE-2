package com.example.demo.news.scheduler;

import com.example.demo.news.application.usecase.CollectNewsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
class NewsCrawlerScheduler {

    private final CollectNewsUseCase collectNewsUseCase;

    @EventListener(ApplicationReadyEvent.class)
    public void collectOnReady() {
        collect();
    }

    @Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
    public void collectBySchedule() {
        collect();
    }

    private void collect() {
        try {
            collectNewsUseCase.execute();
        } catch (final RuntimeException exception) {
            log.warn("[News] crawl failed errorMessage={}", exception.getMessage(), exception);
        }
    }
}
