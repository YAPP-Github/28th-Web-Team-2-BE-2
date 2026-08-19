package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record OnlineChannelPriceResponse(
        Integer channelId,
        String channelName,
        String productName,
        @Schema(description = "수집 단위 기준 가격") Integer price,
        @Schema(description = "기준 단위의 배수. g 품목의 기준은 100g이다", example = "1") Integer quantity,
        @Schema(description = "수집 단위 종류", example = "g") String unit,
        @Schema(description = "채널 간 비교용 100g당 가격") Integer normalizedPrice,
        @Schema(nullable = true) String deliveryNote,
        @Schema(nullable = true) String productUrl,
        @Schema(description = "수집 회차 날짜", example = "2026-08-19") LocalDate collectedAt) {}
