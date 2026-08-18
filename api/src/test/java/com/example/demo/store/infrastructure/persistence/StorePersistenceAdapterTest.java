package com.example.demo.store.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.store.application.result.NearbyStoreCandidate;
import com.example.demo.store.domain.StoreFavorite;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StorePersistenceAdapterTest {

    @Autowired
    private StorePersistenceAdapter adapter;

    @Autowired
    private StoreJpaRepository storeJpaRepository;

    @Autowired
    private StoreFavoriteJpaRepository storeFavoriteJpaRepository;

    @BeforeEach
    void setUp() {
        storeFavoriteJpaRepository.deleteAll();
        storeJpaRepository.deleteAll();
    }

    @Test
    void 같은_Kakao_place를_재동기화하면_로컬_ID와_단골_관계를_보존하고_provider_필드를_갱신한다() {
        final var first = adapter.synchronize(List.of(candidate("kakao-1", "old-name"))).getFirst();
        storeFavoriteJpaRepository.save(new StoreFavorite(7L, first.storeId()));

        final var second = adapter.synchronize(List.of(candidate("kakao-1", "new-name"))).getFirst();

        assertThat(second.storeId()).isEqualTo(first.storeId());
        assertThat(storeJpaRepository.findById(first.storeId()).orElseThrow().storeName())
                .isEqualTo("new-name");
        assertThat(adapter.findLikedStoreIds(7L, List.of(first.storeId())))
                .containsExactly(first.storeId());
        assertThat(adapter.findLikedStoreIds(8L, List.of(first.storeId()))).isEmpty();
    }

    private NearbyStoreCandidate candidate(final String kakaoPlaceId, final String name) {
        return new NearbyStoreCandidate(
                kakaoPlaceId,
                name,
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "address",
                "road-address",
                "phone",
                "place-url",
                100);
    }
}
