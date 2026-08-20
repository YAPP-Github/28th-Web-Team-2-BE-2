package com.example.demo.store.application.query;

import java.math.BigDecimal;

public record StoreDetailQuery(
        Long storeId,
        BigDecimal latitude,
        BigDecimal longitude,
        Long userId) {}
