package com.example.demo.mypage.presentation.dto;

public record FavoriteStoreResponse(
        Long storeId,
        String storeName,
        String storeImageUrl,
        Integer distanceMeters,
        String openStatus,
        String todayBusinessHours,
        boolean isLiked) {}
