package com.example.demo.store.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.port.StorePersistencePort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreCandidate;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GetNearbyStoresUseCaseTest {

    private final NearbyStoreSearchPort nearbyStoreSearchPort = mock(NearbyStoreSearchPort.class);
    private final StorePersistencePort storePersistencePort = mock(StorePersistencePort.class);
    private final GetNearbyStoresUseCase useCase = new GetNearbyStoresUseCase(
            nearbyStoreSearchPort, storePersistencePort);

    @Test
    void 전체_필터_건수를_유지하고_본문은_15건으로_제한한다() {
        final List<NearbyStoreCandidate> candidates = java.util.stream.IntStream.rangeClosed(1, 16)
                .mapToObj(index -> candidate("kakao-" + index, index * 10))
                .toList();
        final List<NearbyStoreResult> stores = java.util.stream.IntStream.rangeClosed(1, 16)
                .mapToObj(index -> result((long) index, index * 10, false))
                .toList();
        final NearbyStoreQuery query = query(false, false, null);
        when(nearbyStoreSearchPort.search(query)).thenReturn(new NearbyStoreSearchResult(candidates));
        when(storePersistencePort.synchronize(candidates)).thenReturn(stores);

        final var result = useCase.execute(query);

        assertThat(result.totalCount()).isEqualTo(16);
        assertThat(result.stores()).hasSize(15);
        assertThat(result.stores()).extracting(NearbyStoreResult::storeId)
                .containsExactlyElementsOf(java.util.stream.LongStream.rangeClosed(1, 15).boxed().toList());
    }

    @Test
    void 반경_밖의_가게는_content와_totalCount에서_동시에_제외한다() {
        final List<NearbyStoreCandidate> candidates = List.of(
                candidate("kakao-in", 100), candidate("kakao-out", 2100));
        final List<NearbyStoreResult> stores = List.of(result(1L, 100, false), result(2L, 2100, false));
        final NearbyStoreQuery query = query(false, false, null);
        when(nearbyStoreSearchPort.search(query)).thenReturn(new NearbyStoreSearchResult(candidates));
        when(storePersistencePort.synchronize(candidates)).thenReturn(stores);

        final var result = useCase.execute(query);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.stores()).extracting(NearbyStoreResult::storeId).containsExactly(1L);
    }

    @Test
    void 거리가_같으면_local_storeId_오름차순으로_정렬한다() {
        final List<NearbyStoreCandidate> candidates = List.of(
                candidate("kakao-3", 100),
                candidate("kakao-1", 200),
                candidate("kakao-2", 100));
        final List<NearbyStoreResult> stores = List.of(
                result(3L, 100, false),
                result(1L, 200, false),
                result(2L, 100, false));
        final NearbyStoreQuery query = query(false, false, null);
        when(nearbyStoreSearchPort.search(query)).thenReturn(new NearbyStoreSearchResult(candidates));
        when(storePersistencePort.synchronize(candidates)).thenReturn(stores);

        final var result = useCase.execute(query);

        assertThat(result.stores()).extracting(NearbyStoreResult::storeId)
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    void onlyLiked는_현재_사용자의_단골만_남기고_isLiked를_계산한다() {
        final NearbyStoreCandidate firstCandidate = candidate("kakao-1", 100);
        final NearbyStoreCandidate secondCandidate = candidate("kakao-2", 200);
        final List<NearbyStoreCandidate> candidates = List.of(firstCandidate, secondCandidate);
        final List<NearbyStoreResult> stores = List.of(result(11L, 100, false), result(12L, 200, false));
        final NearbyStoreQuery query = query(true, true, 7L);
        when(nearbyStoreSearchPort.search(query)).thenReturn(new NearbyStoreSearchResult(candidates));
        when(storePersistencePort.synchronize(candidates)).thenReturn(stores);
        when(storePersistencePort.findLikedStoreIds(7L, List.of(11L, 12L))).thenReturn(Set.of(12L));

        final var result = useCase.execute(query);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.stores()).singleElement().satisfies(store -> {
            assertThat(store.storeId()).isEqualTo(12L);
            assertThat(store.isLiked()).isTrue();
        });
    }

    @Test
    void onlyLiked는_ROLE_USER가_아니면_401이고_provider를_호출하지_않는다() {
        final NearbyStoreQuery query = query(true, false, null);

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.errorType()).isEqualTo(ErrorType.UNAUTHORIZED);
                    assertThat(apiException.httpStatus().value()).isEqualTo(401);
                });
        verifyNoInteractions(nearbyStoreSearchPort, storePersistencePort);
    }

    @Test
    void provider가_실패하면_로컬_upsert를_호출하지_않고_오류를_전파한다() {
        final NearbyStoreQuery query = query(false, false, null);
        final ApiException providerFailure = new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                org.springframework.http.HttpStatus.BAD_GATEWAY);
        when(nearbyStoreSearchPort.search(query)).thenThrow(providerFailure);

        assertThatThrownBy(() -> useCase.execute(query)).isSameAs(providerFailure);
        verifyNoInteractions(storePersistencePort);
    }

    private NearbyStoreQuery query(
            final boolean onlyLiked, final boolean roleUser, final Long userId) {
        return new NearbyStoreQuery(
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                2000,
                onlyLiked,
                roleUser,
                userId);
    }

    private NearbyStoreCandidate candidate(final String kakaoPlaceId, final int distance) {
        return new NearbyStoreCandidate(
                kakaoPlaceId,
                kakaoPlaceId,
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "address",
                "road-address",
                "phone",
                "place-url",
                distance);
    }

    private NearbyStoreResult result(final Long storeId, final int distance, final boolean isLiked) {
        return new NearbyStoreResult(
                storeId,
                "store-" + storeId,
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "address",
                "road-address",
                "phone",
                "place-url",
                distance,
                isLiked);
    }
}
