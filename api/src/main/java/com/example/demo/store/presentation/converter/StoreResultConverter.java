package com.example.demo.store.presentation.converter;

import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoresResult;
import com.example.demo.store.presentation.dto.NearbyStoreResponse;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import org.springframework.stereotype.Component;

@Component
public class StoreResultConverter {

    public NearbyStoresResponse toNearbyStoresResponse(final NearbyStoresResult result) {
        return new NearbyStoresResponse(
                result.totalCount(), result.stores().stream().map(this::toResponse).toList());
    }

    private NearbyStoreResponse toResponse(final NearbyStoreResult result) {
        return new NearbyStoreResponse(
                result.storeId(),
                result.storeName(),
                result.latitude(),
                result.longitude(),
                result.addressName(),
                result.roadAddressName(),
                result.phone(),
                result.placeUrl(),
                result.distanceMeters(),
                result.isLiked());
    }
}
