package com.example.demo.report.application.result;

import java.util.List;

public record StoreReportsResult(
        Long storeId,
        long cheapCount,
        long expensiveCount,
        List<StoreReportResult> reports,
        int page,
        int size,
        boolean hasNext) {}
