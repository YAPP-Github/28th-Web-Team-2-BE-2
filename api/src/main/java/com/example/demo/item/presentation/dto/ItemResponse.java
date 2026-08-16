package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ItemResponse(
        Long itemId,
        String itemName,
        String itemImageUrl,
        @Schema(nullable = true) String defaultUnit,
        Integer price,
        Integer priceGap) {}
