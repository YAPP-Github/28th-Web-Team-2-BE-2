package com.example.demo.report.presentation.dto;

import java.math.BigDecimal;

public record RegionLowestPriceReportResponse(
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
