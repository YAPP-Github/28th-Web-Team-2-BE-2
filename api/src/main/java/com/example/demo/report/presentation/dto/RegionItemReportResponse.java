package com.example.demo.report.presentation.dto;

import java.util.List;

public record RegionItemReportResponse(
        String regionId,
        String regionName,
        Long itemId,
        long totalCount,
        List<UserReportSummaryResponse> reports,
        int page,
        int size,
        boolean hasNext) {}
