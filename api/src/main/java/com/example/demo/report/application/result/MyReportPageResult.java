package com.example.demo.report.application.result;

import java.util.List;

public record MyReportPageResult(
        List<MyReportSummaryResult> reports,
        int page,
        int size,
        long totalCount,
        int totalPages,
        boolean hasNext) {}
