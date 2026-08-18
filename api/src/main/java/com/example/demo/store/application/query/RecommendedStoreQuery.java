package com.example.demo.store.application.query;

import java.math.BigDecimal;

public record RecommendedStoreQuery(
        BigDecimal latitude,
        BigDecimal longitude,
        Long itemId,
        Integer radius) {}
