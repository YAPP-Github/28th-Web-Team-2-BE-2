package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ItemResponse(
        Long itemId,
        String itemName,
        String itemImageUrl,
        @Schema(nullable = true) String defaultUnit,
        Integer price,
        Integer priceGap,
        BigDecimal priceDiffRate,
        boolean isLiked) {}
