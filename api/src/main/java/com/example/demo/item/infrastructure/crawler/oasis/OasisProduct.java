package com.example.demo.item.infrastructure.crawler.oasis;

import java.math.BigDecimal;
import java.net.URI;

public record OasisProduct(
        String externalProductId,
        String name,
        URI productUrl,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        BigDecimal pricePer100g,
        String deliveryNote) {

    public OasisProduct(
            final String externalProductId,
            final String name,
            final URI productUrl,
            final BigDecimal sellingPrice,
            final BigDecimal originalPrice) {
        this(externalProductId, name, productUrl, sellingPrice, originalPrice, null, null);
    }

    public OasisProduct withPricePer100g(final BigDecimal pricePer100g) {
        return new OasisProduct(
                externalProductId, name, productUrl, sellingPrice, originalPrice, pricePer100g, deliveryNote);
    }

    public OasisProduct withDeliveryNote(final String deliveryNote) {
        return new OasisProduct(
                externalProductId, name, productUrl, sellingPrice, originalPrice, pricePer100g, deliveryNote);
    }
}
