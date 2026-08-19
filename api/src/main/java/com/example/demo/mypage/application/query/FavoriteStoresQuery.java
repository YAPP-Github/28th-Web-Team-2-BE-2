package com.example.demo.mypage.application.query;

import java.math.BigDecimal;

public record FavoriteStoresQuery(
        Long userId,
        BigDecimal latitude,
        BigDecimal longitude,
        int page,
        int size) {

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
