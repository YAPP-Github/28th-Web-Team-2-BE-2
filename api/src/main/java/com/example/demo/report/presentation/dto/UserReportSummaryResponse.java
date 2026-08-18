package com.example.demo.report.presentation.dto;

import com.example.demo.report.domain.PriceClassification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UserReportSummaryResponse(
        Long reportId,
        @Schema(nullable = true, description = "가게 없는 제보는 null이다") Long storeId,
        @Schema(nullable = true, description = "가게 없는 제보는 null이다") String storeName,
        Integer price,
        BigDecimal amount,
        String unit,
        @Schema(description = "제보 기준일", example = "2026-08-19") LocalDate reportedAt,
        @Schema(nullable = true, description = "제보 당시 공공가격 대비 차이 스냅샷") Integer priceGap,
        @Schema(nullable = true, description = "제보 당시 공공가격 대비 변동률 스냅샷") BigDecimal priceDiffRate,
        @Schema(nullable = true, description = "priceGap 부호 기준 분류. 비교값이 없으면 null이다")
                PriceClassification classification) {}
