package com.example.demo.store.application.query;

import java.math.BigDecimal;

public record NearbyStoreQuery(
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radius) {}
