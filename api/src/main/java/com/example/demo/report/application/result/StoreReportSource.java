package com.example.demo.report.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreReportSource(
        Long reportId,
        Long itemId,
        String itemName,
        String itemImageUrl,
        Integer price,
        String unit,
        LocalDate reportedDate,
        Integer publicPriceDiff,
        BigDecimal priceDiffRate) {}
