package com.example.demo.mypage.application.result;

import java.math.BigDecimal;

public record FavoriteStoreSource(
        Long storeId,
        String storeName,
        BigDecimal latitude,
        BigDecimal longitude) {}
