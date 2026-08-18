package com.example.demo.report.application.result;

import java.util.List;

public record RegionItemReportResult(
        String regionId,
        Long itemId,
        long totalCount,
        List<UserReportSummaryResult> reports,
        int page,
        int size,
        boolean hasNext) {}
