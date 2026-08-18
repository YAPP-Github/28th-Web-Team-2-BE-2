package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record DailyReportResponse(
        @Schema(description = "주간 내 해당 날짜", example = "2026-08-17") LocalDate reportedAt,
        @Schema(description = "그 날 제보가 있었는지") boolean hasReported,
        @Schema(nullable = true, description = "그 날 가장 최근 제보의 품목. 제보가 없으면 null이다") Long itemId,
        @Schema(nullable = true, description = "그 날 가장 최근 제보의 품목명. 제보가 없으면 null이다")
                String itemName) {}
