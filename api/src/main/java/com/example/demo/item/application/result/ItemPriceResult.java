package com.example.demo.item.application.result;

public record ItemPriceResult(
        Long itemId,
        String itemName,
        String itemImageUrl,
        Integer price,
        Integer priceGap) {}
