package com.example.demo.item.application.result;

public record ItemPriceResult(
        Long itemId,
        String itemName,
        String itemImageUrl,
        Integer price,
        Integer priceGap,
        boolean isLiked) {

    public ItemPriceResult withLiked(final boolean liked) {
        return new ItemPriceResult(itemId, itemName, itemImageUrl, price, priceGap, liked);
    }
}
