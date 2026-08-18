package com.example.demo.store.application.result;

import java.math.BigDecimal;

public record NearbyStoreCandidate(
        String kakaoPlaceId,
        String storeName,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressName,
        String roadAddressName,
        String phone,
        String placeUrl,
        Integer distanceMeters) {}
