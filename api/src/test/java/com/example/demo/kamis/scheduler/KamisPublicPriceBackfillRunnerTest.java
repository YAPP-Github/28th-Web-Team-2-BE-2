package com.example.demo.kamis.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.demo.kamis.application.port.KamisItemCatalogPort;
import com.example.demo.kamis.application.port.KamisPeriodPriceQueryPort;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.port.PublicPriceCommandPort;
import com.example.demo.kamis.application.usecase.KamisHistoricalPublicPriceBackfillUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class KamisPublicPriceBackfillRunnerTest {

    private static final Instant NOW = Instant.parse("2026-08-20T16:30:00Z");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void 실행일_기준_어제까지_1년의_도매가격을_요청한다() throws Exception {
        final KamisHistoricalPublicPriceBackfillUseCase useCase =
                mock(KamisHistoricalPublicPriceBackfillUseCase.class);
        final KamisPublicPriceBackfillRunner runner = new KamisPublicPriceBackfillRunner(
                useCase, List.of("1144010200"), "1101", Clock.fixed(NOW, SEOUL));

        runner.run(null);

        verify(useCase).execute(
                List.of("1144010200"),
                "1101",
                java.time.LocalDate.of(2025, 8, 21),
                java.time.LocalDate.of(2026, 8, 20));
    }

    @Test
    void 여러_리전을_하나의_백필_요청으로_전달한다() throws Exception {
        final RecordingBackfillUseCase useCase = new RecordingBackfillUseCase();
        final List<String> regionIds = List.of("1144010100", "1144010200");
        final KamisPublicPriceBackfillRunner runner = new KamisPublicPriceBackfillRunner(
                useCase, regionIds, "1101", Clock.fixed(NOW, SEOUL));

        runner.run(null);

        assertThat(useCase.regionIds).containsExactlyElementsOf(regionIds);
        assertThat(useCase.executeCount).isOne();
    }

    private static final class RecordingBackfillUseCase extends KamisHistoricalPublicPriceBackfillUseCase {

        private List<String> regionIds = List.of();
        private int executeCount;

        private RecordingBackfillUseCase() {
            super(
                    mock(KamisPriceQueryPort.class),
                    mock(KamisPeriodPriceQueryPort.class),
                    mock(KamisItemCatalogPort.class),
                    mock(PublicPriceCommandPort.class));
        }

        @Override
        public int execute(
                final List<String> regionIds,
                final String countryCode,
                final LocalDate startDate,
                final LocalDate endDate) {
            this.regionIds = regionIds;
            executeCount++;
            return 0;
        }

        @Override
        public int execute(
                final String regionId,
                final String countryCode,
                final LocalDate startDate,
                final LocalDate endDate) {
            throw new AssertionError("region IDs must be processed in one backfill request");
        }
    }
}
