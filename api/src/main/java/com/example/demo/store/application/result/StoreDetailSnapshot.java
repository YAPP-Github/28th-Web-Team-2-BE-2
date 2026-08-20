package com.example.demo.store.application.result;

import java.math.BigDecimal;

public record StoreDetailSnapshot(
        Long storeId,
        String storeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude) {}
