package com.example.demo.report.presentation.dto;

import java.util.List;

public record MyReportPageResponse(
        List<MyReportSummaryResponse> reports,
        int page,
        int size,
        long totalCount,
        int totalPages,
        boolean hasNext) {}
