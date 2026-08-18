package com.example.demo.store.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

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
        @Schema(description = "공공 시세보다 저렴한 상품 수", example = "12")
        int cheapItemCount,
        @Schema(description = "대표 저렴 상품명")
        List<String> itemNames,
        @Schema(description = "대표 상품명에 표시하지 않은 저렴 상품 수", example = "7")
        int remainingItemCount) {}
