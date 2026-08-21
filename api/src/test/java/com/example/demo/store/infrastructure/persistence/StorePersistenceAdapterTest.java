package com.example.demo.store.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.store.application.result.NearbyStoreCandidate;
import com.example.demo.store.application.result.StoreDetailEnrichment;
import com.example.demo.store.domain.StoreFavorite;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
class StorePersistenceAdapterTest {

    @Autowired
    private StorePersistenceAdapter adapter;

    @Autowired
    private StoreJpaRepository storeJpaRepository;

    @Autowired
    private StoreFavoriteJpaRepository storeFavoriteJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        assertThat(storeJpaRepository.findById(first.storeId()).orElseThrow().placeName())
                .isEqualTo("new-name");
        assertThat(adapter.findLikedStoreIds(7L, List.of(first.storeId())))
                .containsExactly(first.storeId());
        assertThat(adapter.findLikedStoreIds(8L, List.of(first.storeId()))).isEmpty();
    }

    @Test
    void 단골_조회_DB_오류는_STORE_SYNC_ERROR와_원인을_보존한다() {
        final StoreFavoriteJpaRepository favoriteRepository = mock(StoreFavoriteJpaRepository.class);
        final DataAccessResourceFailureException cause =
                new DataAccessResourceFailureException("favorite query failed");
        when(favoriteRepository.findStoreIdsByUserIdAndStoreIdIn(7L, List.of(1L)))
                .thenThrow(cause);
        final StorePersistenceAdapter isolatedAdapter = new StorePersistenceAdapter(
                mock(StoreJpaRepository.class),
                favoriteRepository,
                mock(PlatformTransactionManager.class));

        assertThatThrownBy(() -> isolatedAdapter.findLikedStoreIds(7L, List.of(1L)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.STORE_SYNC_ERROR);
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }

    @Test
    void 가게_상세_수집값을_저장하면_다시_조회할_수_있다() {
        final var store = adapter.synchronize(List.of(candidate("kakao-detail", "detail"))).getFirst();
        final var hours = List.of("월 09:00 ~ 18:00", "화 휴무");

        adapter.update(store.storeId(), new StoreDetailEnrichment(
                "https://cdn.example.com/images/store.jpg", hours, "OPEN"));

        final var row = jdbcTemplate.queryForMap(
                "SELECT store_image_url, business_hours, open_status FROM stores WHERE store_id = ?",
                store.storeId());
        assertThat(row.get("store_image_url")).isEqualTo("https://cdn.example.com/images/store.jpg");
        assertThat(row.get("business_hours")).isEqualTo(String.join("\n", hours));
        assertThat(row.get("open_status")).isEqualTo("OPEN");
        assertThat(storeJpaRepository.findById(store.storeId()).orElseThrow().placeName())
                .isEqualTo("detail");
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
