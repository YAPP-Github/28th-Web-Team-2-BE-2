package com.example.demo.store.presentation.converter;

import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoresResult;
import com.example.demo.store.application.result.RecommendedStoreResult;
import com.example.demo.store.application.result.RecommendedStoresResult;
import com.example.demo.store.presentation.dto.NearbyStoreResponse;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import com.example.demo.store.presentation.dto.RecommendedStoreResponse;
import com.example.demo.store.presentation.dto.RecommendedStoresResponse;
import org.springframework.stereotype.Component;

@Component
public class StoreResultConverter {

    public NearbyStoresResponse toNearbyStoresResponse(final NearbyStoresResult result) {
        return new NearbyStoresResponse(
                result.totalCount(), result.stores().stream().map(this::toResponse).toList());
    }

    public RecommendedStoresResponse toRecommendedStoresResponse(
            final RecommendedStoresResult result) {
        return new RecommendedStoresResponse(
                result.totalCount(), result.stores().stream().map(this::toRecommendedStoreResponse).toList());
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

    private RecommendedStoreResponse toRecommendedStoreResponse(
            final RecommendedStoreResult result) {
        return new RecommendedStoreResponse(
                result.storeId(),
                result.storeName(),
                result.latitude(),
                result.longitude(),
                result.addressName(),
                result.roadAddressName(),
                result.phone(),
                result.placeUrl(),
                result.distanceMeters(),
                result.cheapItemCount(),
                result.cheapItems(),
                result.remainingItemCount());
    }
}
