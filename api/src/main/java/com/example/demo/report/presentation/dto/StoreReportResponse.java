package com.example.demo.report.presentation.dto;

import com.example.demo.report.application.result.PriceClassification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreReportResponse(
        Long reportId,
        Long itemId,
        String itemName,
        String itemImageUrl,
        Integer price,
        String unit,
        LocalDate reportedDate,
        Integer publicPriceDiff,
        BigDecimal priceDiffRate,
        PriceClassification priceClassification,
        String reporterNickname,
        @Schema(types = {"string", "null"}) String reporterRank,
        @Schema(types = {"string", "null"}) String reporterProfileColor) {}
