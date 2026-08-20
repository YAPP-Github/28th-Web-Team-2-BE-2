package com.example.demo.report.application.result;

import java.time.LocalDate;

public record MyReportSummaryResult(
        Long reportId,
        String itemName,
        Integer price,
        String unit,
        LocalDate reportedDate,
        String regionId,
        String regionName,
        Integer priceGap) {}
