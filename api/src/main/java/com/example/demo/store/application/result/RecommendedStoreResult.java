package com.example.demo.store.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecommendedStoreResult(
        Long storeId,
        String storeName,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressName,
        String roadAddressName,
        String phone,
        String placeUrl,
        Integer distanceMeters,
        Integer price,
        LocalDate reportedDate,
        BigDecimal priceDiffRate) {}
