package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.report.domain.Store;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendedStoreQueryAdapterTest {

    private final UserReportJpaRepository reportRepository = mock(UserReportJpaRepository.class);
    private final StoreJpaRepository storeRepository = mock(StoreJpaRepository.class);
    private final RecommendedStoreQueryAdapter adapter = new RecommendedStoreQueryAdapter(
            reportRepository, storeRepository);

    @Test
    void 최신_저가_제보와_매장_정보를_추천_소스로_변환한다() {
        final UserReport report = mock(UserReport.class);
        final Store store = mock(Store.class);
        when(report.storeId()).thenReturn(7L);
        when(report.price()).thenReturn(3000);
        when(report.reportDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(report.priceDiffRate()).thenReturn(new BigDecimal("-10.00"));
        when(store.id()).thenReturn(7L);
        when(store.placeName()).thenReturn("장보고 마트");
        when(store.latitude()).thenReturn(new BigDecimal("37.5"));
        when(store.longitude()).thenReturn(new BigDecimal("127.0"));
        when(store.addressName()).thenReturn("주소");
        when(store.roadAddressName()).thenReturn("도로명");
        when(store.phone()).thenReturn("전화");
        when(store.placeUrl()).thenReturn("url");
        when(reportRepository.findLatestCheapReportsByItemId(11L)).thenReturn(List.of(report));
        when(storeRepository.findAllById(List.of(7L))).thenReturn(List.of(store));

        final var result = adapter.findLatestCheapReports(11L);

        assertThat(result).singleElement().satisfies(source -> {
            assertThat(source.storeId()).isEqualTo(7L);
            assertThat(source.storeName()).isEqualTo("장보고 마트");
            assertThat(source.price()).isEqualTo(3000);
            assertThat(source.priceDiffRate()).isEqualByComparingTo("-10.00");
        });
    }

    @Test
    void 좌표가_없는_매장은_추천_소스에서_제외한다() {
        final UserReport report = mock(UserReport.class);
        final Store store = mock(Store.class);
        when(report.storeId()).thenReturn(7L);
        when(store.id()).thenReturn(7L);
        when(store.latitude()).thenReturn(null);
        when(store.longitude()).thenReturn(new BigDecimal("127.0"));
        when(reportRepository.findLatestCheapReportsByItemId(11L)).thenReturn(List.of(report));
        when(storeRepository.findAllById(List.of(7L))).thenReturn(List.of(store));

        assertThat(adapter.findLatestCheapReports(11L)).isEmpty();
    }
}
