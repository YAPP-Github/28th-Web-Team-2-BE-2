package com.example.demo.store.application.result;

import java.math.BigDecimal;
import java.util.List;

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
        int cheapItemCount,
        List<String> itemNames,
        int remainingItemCount) {}
