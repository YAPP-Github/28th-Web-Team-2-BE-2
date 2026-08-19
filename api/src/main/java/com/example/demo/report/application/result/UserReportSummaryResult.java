package com.example.demo.report.application.result;

import com.example.demo.report.domain.PriceClassification;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UserReportSummaryResult(
        Long reportId,
        Long storeId,
        String storeName,
        Integer price,
        BigDecimal amount,
        String unit,
        LocalDate reportedDate,
        Integer priceGap,
        BigDecimal priceDiffRate,
        PriceClassification classification) {}
