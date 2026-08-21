package com.example.demo.report.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.port.StoreReportQueryPort;
import com.example.demo.report.application.query.ReportFilter;
import com.example.demo.report.application.query.StoreReportsQuery;
import com.example.demo.report.application.result.PriceClassification;
import com.example.demo.report.application.result.StoreReportResult;
import com.example.demo.report.application.result.StoreReportSource;
import com.example.demo.report.application.result.StoreReportsQueryResult;
import com.example.demo.report.application.result.StoreReportsResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetStoreReportsUseCaseTest {

    private final StoreReportQueryPort queryPort = mock(StoreReportQueryPort.class);
    private final GetStoreReportsUseCase useCase = new GetStoreReportsUseCase(queryPort);

    @Test
    void 저장된_가격차이_스냅샷으로_분류하고_페이지_메타데이터를_반환한다() {
        final StoreReportsQuery query = new StoreReportsQuery(7L, ReportFilter.CHEAP, 1, 2);
        when(queryPort.find(query)).thenReturn(new StoreReportsQueryResult(
                true,
                3,
                2,
                List.of(new StoreReportSource(
                        11L, 3L, "감자", "image", 900, "1kg", LocalDate.now(), -100,
                        new BigDecimal("-10.00"), 5L, "떡볶이킬러")),
                true));

        final StoreReportsResult result = useCase.execute(query);

        assertThat(result.storeId()).isEqualTo(7L);
        assertThat(result.cheapCount()).isEqualTo(3);
        assertThat(result.expensiveCount()).isEqualTo(2);
        assertThat(result.reports().getFirst().priceClassification()).isEqualTo(PriceClassification.CHEAP);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void 가격차이가_0이면_EQUAL로_분류한다() {
        final StoreReportsQuery query = new StoreReportsQuery(7L, ReportFilter.EXPENSIVE, 0, 20);
        when(queryPort.find(query)).thenReturn(new StoreReportsQueryResult(
                true,
                0,
                0,
                List.of(new StoreReportSource(
                        11L, 3L, "감자", null, 1000, "1kg", LocalDate.now(), 0, BigDecimal.ZERO,
                        11L, null)),
                false));

        assertThat(useCase.execute(query).reports().getFirst().priceClassification())
                .isEqualTo(PriceClassification.EQUAL);
    }

    @Test
    void 활성_제보자는_닉네임과_안정적인_프로필_색상을_반환하고_등급은_null이다() {
        final StoreReportsQuery query = new StoreReportsQuery(7L, ReportFilter.ALL, 0, 20);
        when(queryPort.find(query)).thenReturn(new StoreReportsQueryResult(
                true,
                0,
                0,
                List.of(new StoreReportSource(
                        11L, 3L, "감자", null, 1000, "1kg", LocalDate.now(), 0, BigDecimal.ZERO,
                        5L, "떡볶이킬러")),
                false));

        final StoreReportResult result = useCase.execute(query).reports().getFirst();

        assertThat(result.reporterNickname()).isEqualTo("떡볶이킬러");
        assertThat(result.reporterRank()).isNull();
        assertThat(result.reporterProfileColor()).isEqualTo("BLUE");
    }

    @Test
    void 탈퇴한_제보자는_대체_닉네임과_null_프로필을_반환한다() {
        final StoreReportsQuery query = new StoreReportsQuery(7L, ReportFilter.ALL, 0, 20);
        when(queryPort.find(query)).thenReturn(new StoreReportsQueryResult(
                true,
                0,
                0,
                List.of(new StoreReportSource(
                        11L, 3L, "감자", null, 1000, "1kg", LocalDate.now(), 0, BigDecimal.ZERO,
                        null, null)),
                false));

        final StoreReportResult result = useCase.execute(query).reports().getFirst();

        assertThat(result.reporterNickname()).isEqualTo("탈퇴한 이웃");
        assertThat(result.reporterRank()).isNull();
        assertThat(result.reporterProfileColor()).isNull();
    }

    @Test
    void 기준_가게가_없으면_404를_던진다() {
        final StoreReportsQuery query = new StoreReportsQuery(99L, ReportFilter.CHEAP, 0, 20);
        when(queryPort.find(query)).thenReturn(new StoreReportsQueryResult(false, 0, 0, List.of(), false));

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.NO_RESOURCE_ERROR));
    }
}
