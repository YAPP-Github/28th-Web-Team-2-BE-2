package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.item.domain.Item;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.report.domain.Store;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendedStoreQueryAdapterTest {

    private final UserReportJpaRepository reportRepository = mock(UserReportJpaRepository.class);
    private final StoreJpaRepository storeRepository = mock(StoreJpaRepository.class);
    private final ItemJpaRepository itemRepository = mock(ItemJpaRepository.class);
    private final RecommendedStoreQueryAdapter adapter = new RecommendedStoreQueryAdapter(
            reportRepository, storeRepository, itemRepository);

    @Test
    void 최신_저가_제보와_매장_상품_정보를_추천_소스로_변환한다() {
        final UserReport report = mock(UserReport.class);
        final Store store = mock(Store.class);
        final Item item = mock(Item.class);
        when(report.storeId()).thenReturn(7L);
        when(report.itemId()).thenReturn(11L);
        when(store.id()).thenReturn(7L);
        when(store.placeName()).thenReturn("장보고 마트");
        when(store.latitude()).thenReturn(new BigDecimal("37.5"));
        when(store.longitude()).thenReturn(new BigDecimal("127.0"));
        when(store.addressName()).thenReturn("주소");
        when(store.roadAddressName()).thenReturn("도로명");
        when(store.phone()).thenReturn("전화");
        when(store.placeUrl()).thenReturn("url");
        when(item.id()).thenReturn(11L);
        when(item.name()).thenReturn("양파");
        when(reportRepository.findLatestCheapReports("1121510100")).thenReturn(List.of(report));
        when(storeRepository.findAllById(List.of(7L))).thenReturn(List.of(store));
        when(itemRepository.findAllById(List.of(11L))).thenReturn(List.of(item));

        final var result = adapter.findLatestCheapReports("1121510100");

        assertThat(result).singleElement().satisfies(source -> {
            assertThat(source.storeId()).isEqualTo(7L);
            assertThat(source.storeName()).isEqualTo("장보고 마트");
            assertThat(source.itemName()).isEqualTo("양파");
        });
    }

    @Test
    void 좌표가_없는_매장은_추천_소스에서_제외한다() {
        final UserReport report = mock(UserReport.class);
        final Store store = mock(Store.class);
        final Item item = mock(Item.class);
        when(report.storeId()).thenReturn(7L);
        when(report.itemId()).thenReturn(11L);
        when(store.id()).thenReturn(7L);
        when(store.latitude()).thenReturn(null);
        when(store.longitude()).thenReturn(new BigDecimal("127.0"));
        when(item.id()).thenReturn(11L);
        when(item.name()).thenReturn("양파");
        when(reportRepository.findLatestCheapReports("1121510100")).thenReturn(List.of(report));
        when(storeRepository.findAllById(List.of(7L))).thenReturn(List.of(store));
        when(itemRepository.findAllById(List.of(11L))).thenReturn(List.of(item));

        assertThat(adapter.findLatestCheapReports("1121510100")).isEmpty();
    }
}
