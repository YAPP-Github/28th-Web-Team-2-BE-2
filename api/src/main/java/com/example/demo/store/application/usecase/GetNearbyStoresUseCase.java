package com.example.demo.store.application.usecase;

import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import com.example.demo.store.application.result.NearbyStoresResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetNearbyStoresUseCase {

    private final NearbyStoreSearchPort nearbyStoreSearchPort;

    public NearbyStoresResult execute(final NearbyStoreQuery query) {
        final NearbyStoreSearchResult searchResult = nearbyStoreSearchPort.search(query);
        return new NearbyStoresResult(searchResult.totalCount(), searchResult.stores());
    }
}
