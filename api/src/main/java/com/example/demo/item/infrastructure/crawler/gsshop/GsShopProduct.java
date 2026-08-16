package com.example.demo.item.infrastructure.crawler.gsshop;

import java.math.BigDecimal;
import java.net.URI;

public record GsShopProduct(
        String externalProductId,
        String name,
        URI productUrl,
        BigDecimal sellingPrice,
        BigDecimal pricePer100g,
        String deliveryNote) {

    public GsShopProduct(
            final String externalProductId,
            final String name,
            final URI productUrl,
            final BigDecimal sellingPrice,
            final String deliveryNote) {
        this(externalProductId, name, productUrl, sellingPrice, null, deliveryNote);
    }

    public GsShopProduct withPricePer100g(final BigDecimal pricePer100g) {
        return new GsShopProduct(
                externalProductId, name, productUrl, sellingPrice, pricePer100g, deliveryNote);
    }

    public GsShopProduct withDeliveryNote(final String deliveryNote) {
        return new GsShopProduct(
                externalProductId, name, productUrl, sellingPrice, pricePer100g, deliveryNote);
    }
}
