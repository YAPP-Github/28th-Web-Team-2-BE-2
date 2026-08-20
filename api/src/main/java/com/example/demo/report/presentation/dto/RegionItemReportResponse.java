package com.example.demo.report.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RegionItemReportResponse(
        String regionId,
        @Schema(description = "법정동 코드에 해당하는 지역명") String regionName,
        Long itemId,
        long totalCount,
        List<UserReportSummaryResponse> reports,
        int page,
        int size,
        boolean hasNext) {}
