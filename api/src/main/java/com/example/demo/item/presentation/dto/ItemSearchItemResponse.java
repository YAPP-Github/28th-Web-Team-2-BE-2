package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ItemSearchItemResponse(
        Long itemId,
        String itemName,
        @Schema(nullable = true) String itemImageUrl,
        @Schema(nullable = true, description = "기준일 공공가격") Integer price,
        @Schema(nullable = true, description = "직전 기준일 대비 변동률") BigDecimal priceDiffRate) {}
