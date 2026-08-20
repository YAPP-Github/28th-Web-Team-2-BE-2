package com.example.demo.kamis.scheduler;

import com.example.demo.kamis.application.usecase.CollectKamisPublicPriceUseCase;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
final class KamisPublicPriceScheduler {

    private final CollectKamisPublicPriceUseCase collectKamisPublicPriceUseCase;
    private final String regionId;
    private final String countryCode;
    private final Clock clock;

    @Scheduled(cron = "${kamis.public-price.collection.cron:0 30 1 * * *}", zone = "Asia/Seoul")
    public void collectBySchedule() {
        try {
            collectKamisPublicPriceUseCase.execute(regionId, countryCode, LocalDate.now(clock));
        } catch (RuntimeException exception) {
            log.warn("KAMIS public price collection failed errorType={}",
                    exception.getClass().getSimpleName(), exception);
        }
    }
}
