package com.example.demo.item.presentation.dto;

import java.math.BigDecimal;

public record ItemResponse(
        Long itemId,
        String itemName,
        String itemImageUrl,
        Integer price,
        Integer priceGap,
        BigDecimal priceDiffRate) {}
