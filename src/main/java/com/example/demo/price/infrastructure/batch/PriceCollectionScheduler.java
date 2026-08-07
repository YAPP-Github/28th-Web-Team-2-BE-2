package com.example.demo.price.infrastructure.batch;

import com.example.demo.price.application.usecase.LaunchPriceCollectionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceCollectionScheduler {

    private final LaunchPriceCollectionUseCase launchPriceCollectionUseCase;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void launchAtMidnight() {
        launchPriceCollectionUseCase.executeToday();
    }
}
