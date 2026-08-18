package com.example.demo.store.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.port.StorePersistencePort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoresResult;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetNearbyStoresUseCase {

    private static final int CONTENT_LIMIT = 15;

    private final NearbyStoreSearchPort nearbyStoreSearchPort;
    private final StorePersistencePort storePersistencePort;

    public NearbyStoresResult execute(final NearbyStoreQuery query) {
        validateAuthorization(query);

        final List<NearbyStoreResult> synchronizedStores = storePersistencePort.synchronize(
                nearbyStoreSearchPort.search(query).stores());
        final Set<Long> likedStoreIds = findLikedStoreIds(query, synchronizedStores);
        final List<NearbyStoreResult> filteredStores = synchronizedStores.stream()
                .filter(store -> store.distanceMeters() != null)
                .filter(store -> store.distanceMeters() <= query.radius())
                .filter(store -> !query.onlyLiked() || likedStoreIds.contains(store.storeId()))
                .map(store -> store.withLiked(likedStoreIds.contains(store.storeId())))
                .sorted(Comparator.comparing(NearbyStoreResult::distanceMeters)
                        .thenComparing(NearbyStoreResult::storeId))
                .toList();

        return new NearbyStoresResult(
                filteredStores.size(),
                filteredStores.stream().limit(CONTENT_LIMIT).toList());
    }

    private void validateAuthorization(final NearbyStoreQuery query) {
        if (query.onlyLiked() && (!query.roleUser() || query.userId() == null)) {
            throw new ApiException(
                    ErrorType.UNAUTHORIZED.description(),
                    ErrorType.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private Set<Long> findLikedStoreIds(
            final NearbyStoreQuery query,
            final List<NearbyStoreResult> stores) {
        if (!query.roleUser() || query.userId() == null || stores.isEmpty()) {
            return Set.of();
        }
        return storePersistencePort.findLikedStoreIds(
                query.userId(), stores.stream().map(NearbyStoreResult::storeId).toList());
    }
}
