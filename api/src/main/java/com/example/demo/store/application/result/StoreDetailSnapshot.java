package com.example.demo.store.application.result;

import java.math.BigDecimal;
import java.time.Instant;
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
        String openStatus,
        Instant kakaoDetailsCollectedAt) {

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final String placeUrl,
            final String storeImageUrl,
            final List<String> businessHours,
            final String openStatus) {
        this(storeId, storeName, address, latitude, longitude, placeUrl, storeImageUrl,
                businessHours, openStatus, null);
    }

    public boolean hasKakaoDetails() {
        return storeImageUrl != null
                || businessHours != null
                || (openStatus != null && !"UNKNOWN".equals(openStatus));
    }

    public StoreDetailSnapshot(
            final Long storeId,
            final String storeName,
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        this(storeId, storeName, address, latitude, longitude, null, null, null, "UNKNOWN", null);
    }
}
