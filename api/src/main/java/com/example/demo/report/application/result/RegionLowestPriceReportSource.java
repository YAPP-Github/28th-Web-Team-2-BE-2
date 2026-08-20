package com.example.demo.report.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegionLowestPriceReportSource(
        Long reportId,
        Long itemId,
        String itemName,
        String itemImageUrl,
        Long storeId,
        String storeName,
        Integer price,
        String unit,
        BigDecimal priceDiffRate,
        LocalDate reportedAt) {}
