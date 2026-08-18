package com.example.demo.report.application.command;

import java.math.BigDecimal;

public record StoreSnapshot(
        String kakaoPlaceId,
        String placeName,
        String placeUrl,
        String categoryName,
        String addressName,
        String roadAddressName,
        String phone,
        String categoryGroupCode,
        String categoryGroupName,
        BigDecimal longitude,
        BigDecimal latitude,
        Integer distance) {

    public StoreSnapshot(final String placeName, final String addressName) {
        this(null, placeName, null, null, addressName, null, null, null, null, null, null, null);
    }
}
