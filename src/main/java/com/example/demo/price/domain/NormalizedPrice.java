package com.example.demo.price.domain;

import java.math.BigDecimal;

public record NormalizedPrice(BigDecimal amount, PriceUnit unit, BigDecimal pricePer100g) {

    public NormalizedPrice(final BigDecimal amount, final PriceUnit unit) {
        this(amount, unit, null);
    }

    public NormalizedPrice {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("normalized price must not be negative");
        }
        if (unit == null) {
            throw new IllegalArgumentException("normalized price unit must not be null");
        }
        if (pricePer100g != null && pricePer100g.signum() < 0) {
            throw new IllegalArgumentException("100g price must not be negative");
        }
    }
}
