package com.example.demo.store.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.store.application.port.RecommendedStoreQueryPort;
import com.example.demo.store.application.query.RecommendedStoreQuery;
import com.example.demo.store.application.result.RecommendedStoreSource;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetRecommendedStoresUseCaseTest {

    private final RecommendedStoreQueryPort queryPort = mock(RecommendedStoreQueryPort.class);
    private final GetRecommendedStoresUseCase useCase = new GetRecommendedStoresUseCase(queryPort);

    @Test
    void 매장별_저렴한_상품_수를_집계하고_대표_상품명과_나머지_수를_반환한다() {
        final RecommendedStoreQuery query = query(2000);
        when(queryPort.findLatestCheapReports()).thenReturn(List.of(
                source(1L, "양파", "37.501", "127.001"),
                source(1L, "대추방울토마토", "37.501", "127.001"),
                source(1L, "얼갈이배추", "37.501", "127.001"),
                source(1L, "새송이버섯", "37.501", "127.001"),
                source(1L, "고춧가루", "37.501", "127.001"),
                source(1L, "감자", "37.501", "127.001"),
                source(2L, "양파", "37.510", "127.010"),
                source(3L, "당근", "37.530", "127.030")));

        final var result = useCase.execute(query);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.stores()).extracting("storeId").containsExactly(1L, 2L);
        assertThat(result.stores().getFirst()).satisfies(store -> {
            assertThat(store.cheapItemCount()).isEqualTo(6);
            assertThat(store.itemNames()).containsExactly(
                    "양파", "대추방울토마토", "얼갈이배추", "새송이버섯", "고춧가루");
            assertThat(store.remainingItemCount()).isEqualTo(1);
        });
    }

    private RecommendedStoreQuery query(final int radius) {
        return new RecommendedStoreQuery(new BigDecimal("37.5"), new BigDecimal("127.0"), radius);
    }

    private RecommendedStoreSource source(
            final Long storeId,
            final String itemName,
            final String latitude,
            final String longitude) {
        return new RecommendedStoreSource(
                storeId,
                "store-" + storeId,
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                "address",
                "road-address",
                "phone",
                "place-url",
                itemName);
    }
}
