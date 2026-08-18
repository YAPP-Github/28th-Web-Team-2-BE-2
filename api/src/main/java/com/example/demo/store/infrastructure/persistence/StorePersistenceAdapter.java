package com.example.demo.store.infrastructure.persistence;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.domain.Store;
import com.example.demo.store.application.port.StorePersistencePort;
import com.example.demo.store.application.result.NearbyStoreCandidate;
import com.example.demo.store.application.result.NearbyStoreResult;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class StorePersistenceAdapter implements StorePersistencePort {

    private static final int MAX_SYNC_ATTEMPTS = 2;

    private final StoreJpaRepository storeJpaRepository;
    private final StoreFavoriteJpaRepository storeFavoriteJpaRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public List<NearbyStoreResult> synchronize(final List<NearbyStoreCandidate> candidates) {
        for (int attempt = 1; attempt <= MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                return executeSynchronization(candidates);
            } catch (final DataAccessException exception) {
                if (attempt == MAX_SYNC_ATTEMPTS) {
                    throw storeSyncException(exception);
                }
            } catch (final RuntimeException exception) {
                throw storeSyncException(exception);
            }
        }
        throw storeSyncException();
    }

    private List<NearbyStoreResult> executeSynchronization(
            final List<NearbyStoreCandidate> candidates) {
        // ponytail: retry the complete batch once for a concurrent unique-key race; use a DB-native upsert if contention grows.
        final List<NearbyStoreResult> results = new TransactionTemplate(transactionManager)
                .execute(status -> candidates.stream().map(this::synchronize).toList());
        if (results == null) {
            throw new IllegalStateException("Store synchronization returned no result");
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findLikedStoreIds(final Long userId, final Collection<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Set.of();
        }
        try {
            return storeFavoriteJpaRepository.findStoreIdsByUserIdAndStoreIdIn(userId, storeIds);
        } catch (final DataAccessException exception) {
            throw storeSyncException(exception);
        }
    }

    private NearbyStoreResult synchronize(final NearbyStoreCandidate candidate) {
        final Store store = storeJpaRepository.findByKakaoPlaceId(candidate.kakaoPlaceId())
                .orElseGet(() -> new Store(
                        candidate.kakaoPlaceId(),
                        candidate.storeName(),
                        candidate.placeUrl(),
                        null,
                        candidate.addressName(),
                        candidate.roadAddressName(),
                        candidate.phone(),
                        null,
                        null,
                        candidate.longitude(),
                        candidate.latitude(),
                        candidate.distanceMeters()));
        store.updateNearbyProviderFields(
                candidate.storeName(),
                candidate.placeUrl(),
                candidate.addressName(),
                candidate.roadAddressName(),
                candidate.phone(),
                candidate.longitude(),
                candidate.latitude(),
                candidate.distanceMeters());
        final Store saved = storeJpaRepository.save(store);
        return new NearbyStoreResult(
                saved.id(),
                saved.placeName(),
                saved.latitude(),
                saved.longitude(),
                saved.addressName(),
                saved.roadAddressName(),
                saved.phone(),
                saved.placeUrl(),
                candidate.distanceMeters(),
                false);
    }

    private ApiException storeSyncException() {
        return new ApiException(
                ErrorType.STORE_SYNC_ERROR.description(),
                ErrorType.STORE_SYNC_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ApiException storeSyncException(final Throwable cause) {
        return new ApiException(
                ErrorType.STORE_SYNC_ERROR.description(),
                ErrorType.STORE_SYNC_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                cause);
    }
}
