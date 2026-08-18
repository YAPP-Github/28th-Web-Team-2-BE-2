package com.example.demo.item.presentation.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ItemPageResponse(
        LocalDate baseDate,
        long totalCount,
        Map<String, Long> categoryCounts,
        List<ItemResponse> items,
        int page,
        int size,
        boolean hasNext) {}
