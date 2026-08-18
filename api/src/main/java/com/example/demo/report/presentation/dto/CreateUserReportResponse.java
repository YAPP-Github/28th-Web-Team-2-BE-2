package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CreateUserReportResponse(
        @Schema(
                description = "생성된 제보 ID",
                example = "42",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long reportId,
        @Schema(
                description = "제보한 품목 ID",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long itemId,
        @Schema(description = "저장된 매장 ID", example = "7", nullable = true) Long storeId,
        @Schema(
                description = "제보 생성 시각",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant reportedAt) {}
