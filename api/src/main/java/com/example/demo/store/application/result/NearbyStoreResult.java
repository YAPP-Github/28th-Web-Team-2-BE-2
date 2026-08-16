package com.example.demo.store.application.result;

import java.math.BigDecimal;

public record NearbyStoreResult(
        String storeId,
        String storeName,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressName,
        String roadAddressName,
        String phone,
        String placeUrl,
        Integer distanceMeters,
        boolean isLiked) {}
