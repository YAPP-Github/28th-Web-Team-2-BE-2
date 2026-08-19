package com.example.demo.store.application.query;

import java.math.BigDecimal;

public record RecommendedStoreQuery(
        String regionId,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radius) {}
