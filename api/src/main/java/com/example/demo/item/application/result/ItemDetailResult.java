package com.example.demo.item.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemDetailResult(
        Long itemId,
        String itemName,
        String itemImageUrl,
        String defaultUnit,
        boolean isLiked,
        Integer latestLocalReportPrice,
        Integer todayPublicPrice,
        Integer onlineLowestPrice,
        LocalDate baseDate,
        Integer priceGap,
        BigDecimal priceDiffRate) {}
