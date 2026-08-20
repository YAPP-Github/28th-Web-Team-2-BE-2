package com.example.demo.store.application.result;

import java.math.BigDecimal;

public record StoreDetailSnapshot(
        Long storeId,
        String storeName,
        String address,
        String regionId,
        String regionName,
        BigDecimal latitude,
        BigDecimal longitude) {

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        this(storeId, storeName, address, null, null, latitude, longitude);
    }
}
