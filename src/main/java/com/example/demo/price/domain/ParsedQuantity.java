package com.example.demo.price.domain;

import java.math.BigDecimal;

public record ParsedQuantity(BigDecimal value, PriceUnit unit) {

    public ParsedQuantity {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unit == null) {
            throw new IllegalArgumentException("quantity unit must not be null");
        }
    }
}
