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

    private final RecommendedStoreQueryPort port = mock(RecommendedStoreQueryPort.class);
    private final GetRecommendedStoresUseCase useCase = new GetRecommendedStoresUseCase(port);

    @Test
    void 거리순으로_가게를_묶고_저렴한_품목을_최대_다섯개_반환한다() {
        when(port.findLatestCheapReports()).thenReturn(List.of(
                source(1L, "먼 가게", "먼 품목", "37.51", "127.0"),
                source(1L, "먼 가게", "두번째", "37.51", "127.0"),
                source(1L, "먼 가게", "세번째", "37.51", "127.0"),
                source(1L, "먼 가게", "네번째", "37.51", "127.0"),
                source(1L, "먼 가게", "다섯번째", "37.51", "127.0"),
                source(1L, "먼 가게", "여섯번째", "37.51", "127.0"),
                source(2L, "가까운 가게", "품목", "37.5001", "127.0")));

        final var result = useCase.execute(query(2000));

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.stores()).extracting("storeId").containsExactly(2L, 1L);
        assertThat(result.stores().getLast().itemNames()).hasSize(5);
        assertThat(result.stores().getLast().remainingItemCount()).isEqualTo(1);
    }

    @Test
    void 반경_밖의_가게와_열다섯개_초과_가게를_제외한다() {
        when(port.findLatestCheapReports()).thenReturn(List.of(
                source(1L, "가까운 가게", "품목", "37.5", "127.0"),
                source(2L, "먼 가게", "품목", "37.6", "127.0")));

        final var result = useCase.execute(query(0));

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.stores()).extracting("storeId").containsExactly(1L);
    }

    private RecommendedStoreQuery query(final int radius) {
        return new RecommendedStoreQuery(new BigDecimal("37.5"), new BigDecimal("127.0"), radius);
    }

    private RecommendedStoreSource source(
            final Long storeId, final String name, final String item,
            final String latitude, final String longitude) {
        return new RecommendedStoreSource(storeId, name, new BigDecimal(latitude), new BigDecimal(longitude),
                "주소", "도로명", null, "https://place.map.kakao.com/" + storeId, item);
    }
}
