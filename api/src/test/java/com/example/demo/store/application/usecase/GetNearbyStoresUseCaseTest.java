package com.example.demo.store.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetNearbyStoresUseCaseTest {

    private final NearbyStoreSearchPort searchPort = mock(NearbyStoreSearchPort.class);
    private final GetNearbyStoresUseCase useCase = new GetNearbyStoresUseCase(searchPort);

    @Test
    void 일반_조회는_카카오_전체_개수와_가게_목록을_반환한다() {
        when(searchPort.search(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new NearbyStoreSearchResult(6, stores()));
        final var result = useCase.execute(query());

        assertThat(result.totalCount()).isEqualTo(6);
    }

    private NearbyStoreQuery query() {
        return new NearbyStoreQuery(
                new BigDecimal("37.5"), new BigDecimal("127.0"), 2000);
    }

    private List<NearbyStoreResult> stores() {
        return List.of(
                new NearbyStoreResult(
                        "store-a", "마트A", new BigDecimal("37.5"), new BigDecimal("127.0"),
                        "주소A", "도로명A", "02-0000-0000", "http://place.map.kakao.com/a", 100, false),
                new NearbyStoreResult(
                        "store-b", "마트B", new BigDecimal("37.5"), new BigDecimal("127.0"),
                        "주소B", "도로명B", "02-0000-0001", "http://place.map.kakao.com/b", 200, false));
    }
}
