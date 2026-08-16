package com.example.demo.store.presentation.dto;

import java.math.BigDecimal;

public record NearbyStoreResponse(
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
