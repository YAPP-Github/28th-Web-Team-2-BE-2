package com.example.demo.store.application.port;

import com.example.demo.store.application.result.NearbyStoreCandidate;
import com.example.demo.store.application.result.NearbyStoreResult;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface StorePersistencePort {

    List<NearbyStoreResult> synchronize(List<NearbyStoreCandidate> candidates);

    Set<Long> findLikedStoreIds(Long userId, Collection<Long> storeIds);
}
