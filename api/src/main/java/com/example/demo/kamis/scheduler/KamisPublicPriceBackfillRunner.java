package com.example.demo.kamis.scheduler;

import com.example.demo.kamis.application.usecase.KamisHistoricalPublicPriceBackfillUseCase;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@Slf4j
@RequiredArgsConstructor
final class KamisPublicPriceBackfillRunner implements ApplicationRunner {

    private final KamisHistoricalPublicPriceBackfillUseCase useCase;
    private final List<String> regionIds;
    private final String countryCode;
    private final Clock clock;

    @Override
    public void run(final ApplicationArguments args) {
        final LocalDate endDate = LocalDate.now(clock).minusDays(1);
        final LocalDate startDate = endDate.minusYears(1).plusDays(1);
        final int saved = useCase.execute(regionIds, countryCode, startDate, endDate);
        log.info("KAMIS historical wholesale price backfill completed startDate={} endDate={} regionCount={} saved={}",
                startDate, endDate, regionIds.size(), saved);
    }
}
