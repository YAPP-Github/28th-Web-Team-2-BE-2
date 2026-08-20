package com.example.demo.report.application.result;

import java.math.BigDecimal;

public record RegionLowestPriceReportResult(
        int rank,
        Long reportId,
        Long itemId,
        String itemName,
        String itemImageUrl,
        Long storeId,
        String storeName,
        Integer price,
        String unit,
        BigDecimal priceDiffRate) {}
