package com.example.demo.price.domain;

import java.math.BigDecimal;

public record RawOffer(
        String externalProductId,
        String title,
        String productUrl,
        BigDecimal salePrice,
        BigDecimal shippingFee,
        ParsedQuantity quantity,
        String origin,
        boolean available,
        boolean advertisement) {

    public RawOffer {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("offer title must not be blank");
        }
        if (salePrice == null || salePrice.signum() < 0) {
            throw new IllegalArgumentException("sale price must not be negative");
        }
        if (shippingFee == null || shippingFee.signum() < 0) {
            throw new IllegalArgumentException("shipping fee must not be negative");
        }
    }
}
