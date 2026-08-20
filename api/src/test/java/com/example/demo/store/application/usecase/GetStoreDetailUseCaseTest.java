package com.example.demo.store.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.store.application.port.StoreDetailQueryPort;
import com.example.demo.store.application.port.StoreDetailEnrichmentPort;
import com.example.demo.store.application.query.StoreDetailQuery;
import com.example.demo.store.application.result.StoreDetailSnapshot;
import com.example.demo.store.application.result.StoreReportSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetStoreDetailUseCaseTest {

    private final StoreDetailQueryPort queryPort = Mockito.mock(StoreDetailQueryPort.class);
    private final StoreDetailEnrichmentPort enrichmentPort = Mockito.mock(StoreDetailEnrichmentPort.class);
    private final GetStoreDetailUseCase useCase = new GetStoreDetailUseCase(queryPort, enrichmentPort);

    @Test
    void 좌표와_출처없는_필드는_null이고_영업상태는_UNKNOWN이다() {
        givenStore();
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);

        final var result = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(result.storeImageUrl()).isNull();
        assertThat(result.regionId()).isNull();
        assertThat(result.regionName()).isNull();
        assertThat(result.distance()).isNull();
        assertThat(result.walkTimeMinutes()).isNull();
        assertThat(result.businessHours()).isNull();
        assertThat(result.openStatus()).isEqualTo("UNKNOWN");
        assertThat(result.latestReportedDate()).isNull();
        assertThat(result.latestReportedAt()).isNull();
    }

    @Test
    void 좌표가_있으면_직선거리를_계산하고_도보시간은_환산하지_않는다() {
        givenStore();
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);

        final var result = useCase.execute(new StoreDetailQuery(
                1L,
                new BigDecimal("37.5088"),
                new BigDecimal("127.0732"),
                null));

        assertThat(result.distance()).isBetween(870, 890);
        assertThat(result.walkTimeMinutes()).isNull();
    }

    @Test
    void ROLE_USER의_본인_찜만_조회하고_익명은_찜_조회하지_않는다() {
        givenStore();
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(2L);
        when(queryPort.isLiked(7L, 1L)).thenReturn(true);

        final var userResult = useCase.execute(new StoreDetailQuery(1L, null, null, 7L));
        final var anonymousResult = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(userResult.isLiked()).isTrue();
        assertThat(userResult.favoriteCount()).isEqualTo(2L);
        assertThat(anonymousResult.isLiked()).isFalse();
        verify(queryPort, never()).isLiked(null, 1L);
    }

    @Test
    void 최근_제보_집계를_응답으로_전달한다() {
        givenStore();
        final LocalDate reportedDate = LocalDate.now();
        final Instant reportedAt = Instant.parse("2026-08-19T01:00:00Z");
        when(queryPort.findReportSummary(any(), any()))
                .thenReturn(new StoreReportSummary(3L, 1L, 5L, reportedDate, reportedAt));
        when(queryPort.countFavorites(1L)).thenReturn(4L);

        final var result = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(result.favoriteCount()).isEqualTo(4L);
        assertThat(result.cheapItemCount()).isEqualTo(3L);
        assertThat(result.expensiveItemCount()).isEqualTo(1L);
        assertThat(result.totalReportedItemCount()).isEqualTo(5L);
        assertThat(result.latestReportedDate()).isEqualTo(reportedDate);
        assertThat(result.latestReportedAt()).isEqualTo(reportedAt);
    }

    @Test
    void 저장된_Kakao_상세가_있으면_재수집하지_않는다() {
        when(queryPort.findStore(1L)).thenReturn(java.util.Optional.of(new StoreDetailSnapshot(
                1L, "장보고 마트", "주소", new BigDecimal("37.5"), new BigDecimal("127"),
                "https://place.map.kakao.com/1", "https://s3.example/image.jpg",
                java.util.List.of("월 09:00 - 18:00"), "OPEN")));
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);

        useCase.execute(new StoreDetailQuery(1L, null, null, null));

        verifyNoInteractions(enrichmentPort);
    }

    @Test
    void 빈_수집도_시각을_기록해_연속_조회에서_재수집하지_않는다() {
        final StoreDetailSnapshot original = new StoreDetailSnapshot(
                1L, "가게", "주소", new BigDecimal("37.5"), new BigDecimal("127"),
                "https://place.map.kakao.com/1", null, null, "UNKNOWN");
        final StoreDetailSnapshot attempted = new StoreDetailSnapshot(
                1L, "가게", "주소", new BigDecimal("37.5"), new BigDecimal("127"),
                "https://place.map.kakao.com/1", null, null, "UNKNOWN",
                Instant.parse("2026-08-20T00:00:00Z"));
        when(queryPort.findStore(1L)).thenReturn(java.util.Optional.of(original), java.util.Optional.of(attempted));
        when(enrichmentPort.enrich(original)).thenReturn(original);
        when(queryPort.saveDetails(original)).thenReturn(attempted);
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);

        useCase.execute(new StoreDetailQuery(1L, null, null, null));
        useCase.execute(new StoreDetailQuery(1L, null, null, null));

        verify(enrichmentPort, times(1)).enrich(original);
    }

    @Test
    void 없는_가게는_404_NO_RESOURCE_ERROR다() {
        when(queryPort.findStore(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new StoreDetailQuery(99L, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.errorType()).isEqualTo(ErrorType.NO_RESOURCE_ERROR);
                    assertThat(apiException.httpStatus().value()).isEqualTo(404);
                });
    }

    private void givenStore() {
        when(queryPort.findStore(1L)).thenReturn(java.util.Optional.of(new StoreDetailSnapshot(
                1L,
                "장보고 마트",
                "서울 강남구 삼성동 123",
                new BigDecimal("37.5088"),
                new BigDecimal("127.0632"))));
    }

    private StoreReportSummary emptyReports() {
        return new StoreReportSummary(0L, 0L, 0L, null, null);
    }
}
