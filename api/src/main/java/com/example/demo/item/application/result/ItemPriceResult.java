package com.example.demo.item.application.result;

import java.math.BigDecimal;

public record ItemPriceResult(
        Long itemId,
        String itemName,
        String itemImageUrl,
        Integer price,
        Integer priceGap,
        BigDecimal priceDiffRate) {}
