package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyWeeklyReportResponse(
        @Schema(description = "이번 주 제보한 날 수. 같은 날 여러 건은 하루로 센다") int totalReportedDays,
        @Schema(description = "월요일부터 7일. 제보가 없는 날도 포함한다")
                List<DailyReportResponse> dailyReports) {}
