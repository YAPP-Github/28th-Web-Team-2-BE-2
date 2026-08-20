package com.example.demo.report.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.report.application.port.RegionLowestPriceReportQueryPort;
import com.example.demo.report.application.query.RegionLowestPriceReportsQuery;
import com.example.demo.report.application.result.RegionLowestPriceReportSource;
import com.example.demo.report.application.result.RegionLowestPriceReportsQueryResult;
import com.example.demo.report.application.result.RegionLowestPriceReportsResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetRegionLowestPriceReportsUseCaseTest {

    private final RegionLowestPriceReportQueryPort queryPort = mock(RegionLowestPriceReportQueryPort.class);
    private final GetRegionLowestPriceReportsUseCase useCase = new GetRegionLowestPriceReportsUseCase(queryPort);

    @Test
    void 입력_순서와_무관하게_품목별_최저가를_선택하고_할인율이_없는_제보는_제외한다() {
        when(queryPort.find("9999999999", LocalDate.now().minusDays(6), LocalDate.now()))
                .thenReturn(new RegionLowestPriceReportsQueryResult(
                        true,
                        "테스트 지역",
                        List.of(
                                new RegionLowestPriceReportSource(
                                        101L, 1L, "감자", null, null, null, 800, "1kg",
                                        new BigDecimal("-60.00"), LocalDate.of(2026, 8, 19)),
                                new RegionLowestPriceReportSource(
                                        102L, 1L, "감자", null, null, null, 700, "1kg",
                                        new BigDecimal("-30.00"), LocalDate.of(2026, 8, 20)),
                                new RegionLowestPriceReportSource(
                                        201L, 2L, "양파", null, null, null, 100, "1kg", null,
                                        LocalDate.of(2026, 8, 20)),
                                new RegionLowestPriceReportSource(
                                        301L, 3L, "당근", null, null, null, 900, "1kg",
                                        new BigDecimal("-50.00"), LocalDate.of(2026, 8, 20)))));

        final RegionLowestPriceReportsResult result = useCase.execute(
                new RegionLowestPriceReportsQuery("9999999999", 5));

        assertThat(result.items())
                .extracting(item -> item.itemId())
                .containsExactly(3L, 1L);
        assertThat(result.items().get(1).price()).isEqualTo(700);
    }
}
