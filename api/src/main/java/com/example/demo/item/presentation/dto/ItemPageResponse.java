package com.example.demo.item.presentation.dto;

import java.time.LocalDate;
import java.util.List;

public record ItemPageResponse(
        LocalDate baseDate,
        long totalCount,
        List<ItemResponse> items,
        int page,
        int size,
        boolean hasNext) {}
