package com.example.demo.store.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecommendedStoreResponse(
        Long storeId,
        String storeName,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressName,
        String roadAddressName,
        String phone,
        String placeUrl,
        Integer distanceMeters,
        @Schema(description = "최신 제보 가격", example = "3000")
        Integer reportedPrice,
        @Schema(description = "최신 가격 제보일", example = "2026-08-01")
        LocalDate reportedDate,
        @Schema(description = "공시가격 대비 가격 차이율", example = "-10.00")
        BigDecimal priceDiffRate) {}
