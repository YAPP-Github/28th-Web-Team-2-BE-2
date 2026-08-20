package com.example.demo.item.scheduler;

import com.example.demo.item.application.usecase.CollectOnlinePriceUseCase;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
class OnlinePriceScheduler {

    private final CollectOnlinePriceUseCase collectOnlinePriceUseCase;
    private final Clock clock;
    private final Executor executor;
    private boolean running;
    private boolean pending;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void collectBySchedule() {
        synchronized (this) {
            if (running) {
                pending = true;
                return;
            }
            running = true;
        }
        submitCollection();
    }

    private void submitCollection() {
        try {
            executor.execute(this::collectUntilNoPending);
        } catch (RuntimeException exception) {
            synchronized (this) {
                running = false;
                pending = false;
            }
            log.warn(
                    "online price collection scheduler submission failed errorType={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void collectUntilNoPending() {
        while (true) {
            try {
                collectOnlinePriceUseCase.execute(LocalDate.now(clock));
            } catch (RuntimeException exception) {
                log.warn(
                        "online price collection scheduler failed errorType={}",
                        exception.getClass().getSimpleName());
            }
            synchronized (this) {
                if (!pending) {
                    running = false;
                    return;
                }
                pending = false;
            }
        }
    }
}
