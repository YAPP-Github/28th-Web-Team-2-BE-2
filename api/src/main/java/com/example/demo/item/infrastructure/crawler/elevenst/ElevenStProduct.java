package com.example.demo.item.infrastructure.crawler.elevenst;

import java.math.BigDecimal;
import java.net.URI;

public record ElevenStProduct(
        String externalProductId,
        String name,
        URI productUrl,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        BigDecimal pricePer100g,
        String deliveryNote) {

    public ElevenStProduct(
            final String externalProductId,
            final String name,
            final URI productUrl,
            final BigDecimal sellingPrice,
            final BigDecimal originalPrice) {
        this(externalProductId, name, productUrl, sellingPrice, originalPrice, null, null);
    }

    public ElevenStProduct withPricePer100g(final BigDecimal pricePer100g) {
        return new ElevenStProduct(
                externalProductId, name, productUrl, sellingPrice, originalPrice, pricePer100g, deliveryNote);
    }

    public ElevenStProduct withDeliveryNote(final String deliveryNote) {
        return new ElevenStProduct(
                externalProductId, name, productUrl, sellingPrice, originalPrice, pricePer100g, deliveryNote);
    }
}
