package com.example.demo.store.application.result;

import java.math.BigDecimal;
import java.util.List;

public record StoreDetailSnapshot(
        Long storeId,
        String storeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl,
        String storeImageUrl,
        List<String> businessHours,
        String openStatus) {

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        this(storeId, storeName, address, latitude, longitude, null, null, null, "UNKNOWN");
    }
}
