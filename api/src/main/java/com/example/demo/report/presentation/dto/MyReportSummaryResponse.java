package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record MyReportSummaryResponse(
        Long reportId,
        @Schema(nullable = true, description = "삭제된 품목이면 null이다") String itemName,
        Integer price,
        String unit,
        @Schema(description = "제보 기준일", example = "2026-08-19") LocalDate reportedDate,
        @Schema(description = "제보 당시 법정동 코드 스냅샷", example = "1121510100") String regionId,
        @Schema(nullable = true, description = "법정동 코드에 해당하는 지역명. 참조 데이터에 없으면 null이다")
                String regionName,
        @Schema(nullable = true, description = "제보 당시 공공가격 대비 차이 스냅샷") Integer priceGap) {}
