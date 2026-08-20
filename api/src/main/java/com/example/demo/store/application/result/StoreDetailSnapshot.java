package com.example.demo.store.application.result;

import java.math.BigDecimal;

public record StoreDetailSnapshot(
        Long storeId,
        String storeName,
        String address,
        String regionId,
        String regionName,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl) {

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final String regionId,
            final String regionName,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        this(storeId, storeName, address, regionId, regionName, latitude, longitude, null);
    }

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        this(storeId, storeName, address, null, null, latitude, longitude, null);
    }
}
