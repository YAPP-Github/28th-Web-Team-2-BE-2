package com.example.demo.store.application.result;

import java.math.BigDecimal;
import java.util.List;

public record StoreDetailSnapshot(
        Long storeId,
        String storeName,
        String address,
        String regionId,
        String regionName,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl,
        String storeImageUrl,
        List<String> businessHours,
        String openStatus) {

    public StoreDetailSnapshot {
        businessHours = businessHours == null ? List.of() : List.copyOf(businessHours);
        openStatus = openStatus == null ? "UNKNOWN" : openStatus;
    }

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final String regionId,
            final String regionName,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        this(storeId, storeName, address, regionId, regionName, latitude, longitude,
                null, null, List.of(), "UNKNOWN");
    }

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final String regionId,
            final String regionName,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final String placeUrl) {
        this(storeId, storeName, address, regionId, regionName, latitude, longitude,
                placeUrl, null, List.of(), "UNKNOWN");
    }

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        this(storeId, storeName, address, null, null, latitude, longitude,
                null, null, List.of(), "UNKNOWN");
    }
}
