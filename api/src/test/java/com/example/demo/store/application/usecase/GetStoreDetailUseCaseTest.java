package com.example.demo.store.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.application.usecase.UploadImageUseCase;
import com.example.demo.image.domain.ImageKey;
import com.example.demo.store.application.port.StoreDetailPersistencePort;
import com.example.demo.store.application.port.StoreDetailQueryPort;
import com.example.demo.store.application.port.StorePageSource;
import com.example.demo.store.application.query.StoreDetailQuery;
import com.example.demo.store.application.result.StoreDetailEnrichment;
import com.example.demo.store.application.result.StoreDetailSnapshot;
import com.example.demo.store.application.result.StorePageContent;
import com.example.demo.store.application.result.StoreReportSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetStoreDetailUseCaseTest {

    private final StoreDetailQueryPort queryPort = Mockito.mock(StoreDetailQueryPort.class);
    private final StorePageSource storePageSource = Mockito.mock(StorePageSource.class);
    private final StoreDetailPersistencePort persistencePort = Mockito.mock(StoreDetailPersistencePort.class);
    private final UploadImageUseCase uploadImageUseCase = Mockito.mock(UploadImageUseCase.class);
    private final GetStoreDetailUseCase useCase = new GetStoreDetailUseCase(
            queryPort, storePageSource, persistencePort, uploadImageUseCase);

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
        assertThat(result.businessHours()).isEmpty();
        assertThat(result.openStatus()).isEqualTo("UNKNOWN");
        assertThat(result.latestReportedDate()).isNull();
        assertThat(result.latestReportedAt()).isNull();
        verify(storePageSource, never()).find(any());
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
    void Kakao_장소_페이지의_이미지를_S3_영구_URL로_저장하고_응답한다() {
        givenStoreWithPlaceUrl();
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);
        when(storePageSource.find("https://place.map.kakao.com/123"))
                .thenReturn(pageWithPng(List.of(), "UNKNOWN"));
        when(uploadImageUseCase.execute(any(ImageKey.class), any(UploadImageCommand.class)))
                .thenReturn(new UploadedImageResult("https://cdn.example.com/images/store.png"));

        final var result = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(result.storeImageUrl()).isEqualTo("https://cdn.example.com/images/store.png");
        verify(persistencePort).update(
                eq(1L),
                eq(new StoreDetailEnrichment(
                        "https://cdn.example.com/images/store.png", null, null)));
    }

    @Test
    void Kakao_페이지의_영업시간과_영업상태를_응답하고_저장한다() {
        givenStoreWithPlaceUrl();
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);
        final List<String> hours = List.of("월 09:00 ~ 18:00", "화 09:00 ~ 18:00");
        when(storePageSource.find("https://place.map.kakao.com/123"))
                .thenReturn(new StorePageContent(null, null, null, hours, "OPEN"));

        final var result = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(result.businessHours()).containsExactlyElementsOf(hours);
        assertThat(result.openStatus()).isEqualTo("OPEN");
        verify(persistencePort).update(eq(1L), eq(new StoreDetailEnrichment(null, hours, "OPEN")));
    }

    @Test
    void Kakao_페이지_수집이_실패해도_기본_상세_응답을_반환한다() {
        givenStoreWithPlaceUrl();
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);
        when(storePageSource.find("https://place.map.kakao.com/123"))
                .thenThrow(new IllegalStateException("fetch failed"));

        final var result = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(result.storeImageUrl()).isNull();
        assertThat(result.storeName()).isEqualTo("장보고 마트");
        assertThat(result.businessHours()).isEmpty();
        assertThat(result.openStatus()).isEqualTo("UNKNOWN");
        verify(persistencePort, never()).update(any(), any());
    }

    @Test
    void 수집에_실패하면_이미_저장한_상세_필드를_보존한다() {
        when(queryPort.findStore(1L)).thenReturn(java.util.Optional.of(new StoreDetailSnapshot(
                1L,
                "장보고 마트",
                "주소",
                null,
                null,
                new BigDecimal("37.5"),
                new BigDecimal("127"),
                "https://place.map.kakao.com/123",
                "https://cdn.example.com/store.jpg",
                List.of("월 09:00 ~ 18:00"),
                "OPEN")));
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);
        when(storePageSource.find("https://place.map.kakao.com/123"))
                .thenThrow(new IllegalStateException("fetch failed"));

        final var result = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(result.storeImageUrl()).isEqualTo("https://cdn.example.com/store.jpg");
        assertThat(result.businessHours()).containsExactly("월 09:00 ~ 18:00");
        assertThat(result.openStatus()).isEqualTo("OPEN");
        verify(persistencePort, never()).update(any(), any());
    }

    @Test
    void S3_업로드가_실패해도_영업시간과_영업상태는_응답하고_저장한다() {
        givenStoreWithPlaceUrl();
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);
        final List<String> hours = List.of("매일 10:00 ~ 20:00");
        when(storePageSource.find("https://place.map.kakao.com/123"))
                .thenReturn(pageWithPng(hours, "CLOSED"));
        when(uploadImageUseCase.execute(any(ImageKey.class), any(UploadImageCommand.class)))
                .thenThrow(new IllegalStateException("S3 unavailable"));

        final var result = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(result.storeImageUrl()).isNull();
        assertThat(result.businessHours()).containsExactlyElementsOf(hours);
        assertThat(result.openStatus()).isEqualTo("CLOSED");
        verify(persistencePort).update(eq(1L), eq(new StoreDetailEnrichment(null, hours, "CLOSED")));
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
        when(queryPort.findReportSummary(eq(1L), any(LocalDate.class)))
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
    void 저장된_지역을_응답으로_전달한다() {
        when(queryPort.findStore(1L)).thenReturn(java.util.Optional.of(new StoreDetailSnapshot(
                1L, "장보고 마트", "주소", "1121510100", "서울특별시 광진구 중곡동",
                new BigDecimal("37.5"), new BigDecimal("127"))));
        when(queryPort.findReportSummary(any(), any())).thenReturn(emptyReports());
        when(queryPort.countFavorites(1L)).thenReturn(0L);

        final var result = useCase.execute(new StoreDetailQuery(1L, null, null, null));

        assertThat(result.regionId()).isEqualTo("1121510100");
        assertThat(result.regionName()).isEqualTo("서울특별시 광진구 중곡동");
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

    private void givenStoreWithPlaceUrl() {
        when(queryPort.findStore(1L)).thenReturn(java.util.Optional.of(new StoreDetailSnapshot(
                1L,
                "장보고 마트",
                "서울 강남구 삼성동 123",
                null,
                null,
                new BigDecimal("37.5088"),
                new BigDecimal("127.0632"),
                "https://place.map.kakao.com/123")));
    }

    private StorePageContent pageWithPng(final List<String> hours, final String openStatus) {
        return new StorePageContent(
                "https://img1.kakaocdn.net/store.png",
                "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                hours,
                openStatus);
    }

    private StoreReportSummary emptyReports() {
        return new StoreReportSummary(0L, 0L, 0L, null, null);
    }
}
