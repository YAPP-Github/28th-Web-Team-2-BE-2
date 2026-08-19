package com.example.demo.store.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StoreDetailResult(
        Long storeId,
        String storeName,
        String storeImageUrl,
        boolean isLiked,
        long favoriteCount,
        long cheapItemCount,
        long expensiveItemCount,
        long totalReportedItemCount,
        String regionId,
        String regionName,
        LocalDate latestReportedDate,
        Instant latestReportedAt,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer distance,
        Integer walkTimeMinutes,
        List<String> businessHours,
        String openStatus) {}
