package com.example.demo.item.application.result;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Objects;

public record OnlinePriceCrawlResult(
        String itemName,
        String productName,
        BigDecimal price,
        int unit,
        URI productUrl,
        String deliveryNote) {

    public static final int PER_100_GRAMS = 100;

    public OnlinePriceCrawlResult {
        Objects.requireNonNull(itemName, "itemName must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(productUrl, "productUrl must not be null");
    }
}
