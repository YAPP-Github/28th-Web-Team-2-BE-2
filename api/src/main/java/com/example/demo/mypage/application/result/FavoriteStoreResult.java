package com.example.demo.mypage.application.result;

public record FavoriteStoreResult(
        Long storeId,
        String storeName,
        String storeImageUrl,
        Integer distanceMeters,
        String openStatus,
        String todayBusinessHours,
        boolean isLiked) {}
