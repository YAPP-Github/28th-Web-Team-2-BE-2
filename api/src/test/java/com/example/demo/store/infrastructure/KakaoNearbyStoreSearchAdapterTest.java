package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoPlace;
import com.example.demo.external.kakao.feign.KakaoMapClient;
import com.example.demo.store.application.query.NearbyStoreQuery;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class KakaoNearbyStoreSearchAdapterTest {

    private final KakaoMapClient kakaoMapClient = mock(KakaoMapClient.class);
    private final KakaoNearbyStoreSearchAdapter adapter = new KakaoNearbyStoreSearchAdapter(kakaoMapClient);

    @Test
    void 카카오_응답을_주변_매장_결과로_변환한다() {
        when(kakaoMapClient.searchCategory(
                any(String.class), any(BigDecimal.class), any(BigDecimal.class), anyInt(),
                any(String.class), anyInt()))
                .thenReturn(new KakaoCategorySearchResult(1, List.of(new KakaoPlace(
                        "123", "강남마트", new BigDecimal("37.4979"), new BigDecimal("127.0276"),
                        "서울 강남구 삼성동 123", "서울 강남구 테헤란로 123", "02-1234-5678",
                        "http://place.map.kakao.com/123", 670))));

        final var result = adapter.search(new NearbyStoreQuery(
                new BigDecimal("37.4979"), new BigDecimal("127.0276"), 1500));

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.stores())
                .singleElement()
                .satisfies(store -> {
                    assertThat(store.storeId()).isEqualTo("123");
                    assertThat(store.storeName()).isEqualTo("강남마트");
                    assertThat(store.latitude()).isEqualByComparingTo("37.4979");
                    assertThat(store.longitude()).isEqualByComparingTo("127.0276");
                    assertThat(store.phone()).isEqualTo("02-1234-5678");
                    assertThat(store.placeUrl()).isEqualTo("http://place.map.kakao.com/123");
                    assertThat(store.distanceMeters()).isEqualTo(670);
                    assertThat(store.isLiked()).isFalse();
                });
    }
}
