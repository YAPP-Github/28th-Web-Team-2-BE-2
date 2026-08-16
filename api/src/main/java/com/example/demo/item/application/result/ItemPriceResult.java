package com.example.demo.item.application.result;

public record ItemPriceResult(
        Long itemId,
        String itemName,
        String itemImageUrl,
        String defaultUnit,
        Integer price,
        Integer priceGap) {}
