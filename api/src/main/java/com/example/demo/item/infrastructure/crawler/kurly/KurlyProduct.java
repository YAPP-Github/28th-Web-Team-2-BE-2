package com.example.demo.item.infrastructure.crawler.kurly;

import java.math.BigDecimal;
import java.net.URI;

public record KurlyProduct(
        String externalProductId,
        String name,
        URI productUrl,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        BigDecimal pricePer100g,
        String deliveryNote) {

    public KurlyProduct(
            final String externalProductId,
            final String name,
            final URI productUrl,
            final BigDecimal sellingPrice,
            final BigDecimal originalPrice) {
        this(externalProductId, name, productUrl, sellingPrice, originalPrice, null, null);
    }

    public KurlyProduct withPricePer100g(final BigDecimal pricePer100g) {
        return new KurlyProduct(
                externalProductId, name, productUrl, sellingPrice, originalPrice, pricePer100g, deliveryNote);
    }

    public KurlyProduct withDeliveryNote(final String deliveryNote) {
        return new KurlyProduct(
                externalProductId, name, productUrl, sellingPrice, originalPrice, pricePer100g, deliveryNote);
    }
}
