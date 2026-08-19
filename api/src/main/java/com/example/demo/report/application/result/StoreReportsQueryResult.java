package com.example.demo.report.application.result;

import java.util.List;

public record StoreReportsQueryResult(
        boolean storeExists,
        long cheapCount,
        long expensiveCount,
        List<StoreReportSource> reports,
        boolean hasNext) {}
