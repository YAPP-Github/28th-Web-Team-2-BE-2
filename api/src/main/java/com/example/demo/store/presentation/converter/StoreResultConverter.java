package com.example.demo.store.presentation.converter;

import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoresResult;
import com.example.demo.store.application.result.RecommendedStoreResult;
import com.example.demo.store.application.result.RecommendedStoresResult;
import com.example.demo.store.application.result.StoreDetailResult;
import com.example.demo.store.presentation.dto.NearbyStoreResponse;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import com.example.demo.store.presentation.dto.RecommendedStoreResponse;
import com.example.demo.store.presentation.dto.RecommendedStoresResponse;
import com.example.demo.store.presentation.dto.StoreDetailResponse;
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

    public StoreDetailResponse toStoreDetailResponse(final StoreDetailResult result) {
        return new StoreDetailResponse(
                result.storeId(),
                result.storeName(),
                result.storeImageUrl(),
                result.isLiked(),
                result.favoriteCount(),
                result.cheapItemCount(),
                result.expensiveItemCount(),
                result.totalReportedItemCount(),
                result.regionId(),
                result.regionName(),
                result.latestReportedDate(),
                result.latestReportedAt(),
                result.address(),
                result.latitude(),
                result.longitude(),
                result.distance(),
                result.walkTimeMinutes(),
                result.businessHours(),
                result.openStatus());
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
