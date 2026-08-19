package com.example.demo.report.presentation.dto;

import java.util.List;

public record StoreReportsResponse(
        Long storeId,
        StoreReportsSummaryResponse summary,
        List<StoreReportResponse> reports,
        int page,
        int size,
        boolean hasNext) {}
