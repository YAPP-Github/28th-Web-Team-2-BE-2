package com.example.demo.item.application.result;

import java.time.LocalDate;
import java.util.List;

public record ItemQueryResult(
        LocalDate baseDate,
        long totalCount,
        List<ItemPriceResult> items,
        int page,
        int size,
        boolean hasNext) {}
