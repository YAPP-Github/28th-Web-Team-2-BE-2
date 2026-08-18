package com.example.demo.item.application.result;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ItemQueryResult(
        LocalDate baseDate,
        long totalCount,
        Map<String, Long> categoryCounts,
        List<ItemPriceResult> items,
        int page,
        int size,
        boolean hasNext) {}
