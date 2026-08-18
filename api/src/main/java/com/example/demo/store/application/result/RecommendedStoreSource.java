package com.example.demo.store.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecommendedStoreSource(
        Long storeId,
        String storeName,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressName,
        String roadAddressName,
        String phone,
        String placeUrl,
        String itemName) {}
