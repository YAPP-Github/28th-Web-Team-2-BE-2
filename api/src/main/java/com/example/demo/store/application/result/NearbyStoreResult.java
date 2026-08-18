package com.example.demo.store.application.result;

import java.math.BigDecimal;

public record NearbyStoreResult(
        Long storeId,
        String storeName,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressName,
        String roadAddressName,
        String phone,
        String placeUrl,
        Integer distanceMeters,
        boolean isLiked) {

    public NearbyStoreResult withLiked(final boolean liked) {
        return new NearbyStoreResult(
                storeId,
                storeName,
                latitude,
                longitude,
                addressName,
                roadAddressName,
                phone,
                placeUrl,
                distanceMeters,
                liked);
    }
}
