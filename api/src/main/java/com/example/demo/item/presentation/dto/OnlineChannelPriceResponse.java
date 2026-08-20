package com.example.demo.item.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record OnlineChannelPriceResponse(
        Integer channelId,
        String channelName,
        @Schema(nullable = true, description = "채널 성격", example = "새벽배송") String channelKind,
        String productName,
        @Schema(description = "unit 기준 가격") Integer price,
        @Schema(description = "가격의 기준 단위. 품목 기준 단위로 환산할 수 없으면 수집 기준인 100g이다", example = "1kg")
                String unit,
        @Schema(nullable = true) String deliveryNote,
        @Schema(nullable = true) String productUrl,
        @Schema(description = "수집 회차 날짜", example = "2026-08-19") LocalDate collectedAt) {}
