package com.example.demo.kamis.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.demo.kamis.application.usecase.CollectKamisPublicPriceUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class KamisPublicPriceSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-08-20T16:30:00Z");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void 설정된_지역과_KAMIS_코드로_서울_기준일을_전달한다() {
        final CollectKamisPublicPriceUseCase useCase = mock(CollectKamisPublicPriceUseCase.class);
        final KamisPublicPriceScheduler scheduler = new KamisPublicPriceScheduler(
                useCase,
                "1144010200",
                "1101",
                Clock.fixed(NOW, SEOUL));

        scheduler.collectBySchedule();

        verify(useCase).execute("1144010200", "1101", LocalDate.of(2026, 8, 21));
    }

    @Test
    void 외부_수집_실패는_스케줄러_예외로_전파하지_않는다() {
        final CollectKamisPublicPriceUseCase useCase = mock(CollectKamisPublicPriceUseCase.class);
        doThrow(new IllegalStateException("KAMIS unavailable"))
                .when(useCase).execute(any(), any(), any());
        final KamisPublicPriceScheduler scheduler = new KamisPublicPriceScheduler(
                useCase,
                "1144010200",
                "1101",
                Clock.fixed(NOW, SEOUL));

        scheduler.collectBySchedule();

        verify(useCase).execute("1144010200", "1101", LocalDate.of(2026, 8, 21));
    }
}
