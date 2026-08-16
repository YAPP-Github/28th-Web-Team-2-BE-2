package com.example.demo.store.presentation.converter;

import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import org.springframework.stereotype.Component;

@Component
public class StoreQueryConverter {

    public NearbyStoreQuery toNearbyStoreQuery(final NearbyStoreRequest request) {
        return new NearbyStoreQuery(
                request.latitude(),
                request.longitude(),
                request.radius());
    }
}
