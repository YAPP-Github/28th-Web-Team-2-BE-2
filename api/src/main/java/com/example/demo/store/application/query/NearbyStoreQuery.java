package com.example.demo.store.application.query;

import java.math.BigDecimal;

public record NearbyStoreQuery(
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radius,
        boolean onlyLiked,
        boolean roleUser,
        Long userId,
        String keyword) {

    public boolean hasKeyword() {
        return keyword != null && !keyword.isEmpty();
    }
}
