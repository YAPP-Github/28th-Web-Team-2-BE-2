package com.example.demo.store.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.store.application.port.RecommendedStoreQueryPort;
import com.example.demo.store.application.query.RecommendedStoreQuery;
import com.example.demo.store.application.result.RecommendedStoreSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetRecommendedStoresUseCaseTest {

    private final RecommendedStoreQueryPort queryPort = mock(RecommendedStoreQueryPort.class);
    private final GetRecommendedStoresUseCase useCase = new GetRecommendedStoresUseCase(queryPort);

    @Test
    void 반경_안의_저가_제보_매장을_거리순으로_최대_15개_반환한다() {
        final RecommendedStoreQuery query = query(2000);
        when(queryPort.findLatestCheapReports(11L)).thenReturn(List.of(
                source(1L, "37.501", "127.001"),
                source(2L, "37.510", "127.010"),
                source(3L, "37.530", "127.030")));

        final var result = useCase.execute(query);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.stores()).extracting("storeId").containsExactly(1L, 2L);
        assertThat(result.stores().getFirst().priceDiffRate()).isNegative();
    }

    @Test
    void 같은_거리면_storeId_오름차순으로_정렬한다() {
        final RecommendedStoreQuery query = query(2000);
        when(queryPort.findLatestCheapReports(11L)).thenReturn(List.of(
                source(2L, "37.501", "127.001"),
                source(1L, "37.501", "127.001")));

        final var result = useCase.execute(query);

        assertThat(result.stores()).extracting("storeId").containsExactly(1L, 2L);
    }

    private RecommendedStoreQuery query(final int radius) {
        return new RecommendedStoreQuery(
                new BigDecimal("37.5"), new BigDecimal("127.0"), 11L, radius);
    }

    private RecommendedStoreSource source(
            final Long storeId, final String latitude, final String longitude) {
        return new RecommendedStoreSource(
                storeId,
                "store-" + storeId,
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                "address",
                "road-address",
                "phone",
                "place-url",
                3000,
                LocalDate.of(2026, 8, 1),
                new BigDecimal("-10.00"));
    }
}
