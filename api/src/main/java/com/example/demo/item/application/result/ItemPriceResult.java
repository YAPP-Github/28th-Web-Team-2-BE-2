package com.example.demo.item.application.result;

import java.math.BigDecimal;

public record ItemPriceResult(
        Long itemId,
        String itemName,
        String itemImageUrl,
        String defaultUnit,
        Integer price,
        Integer priceGap,
        BigDecimal priceDiffRate,
        boolean isLiked) {

    public ItemPriceResult withLiked(final boolean liked) {
        return new ItemPriceResult(
                itemId, itemName, itemImageUrl, defaultUnit, price, priceGap, priceDiffRate, liked);
    }
}
