package com.example.demo.item.presentation.dto;

import java.util.List;

public record ItemSearchResponse(
        long totalCount, List<ItemSearchItemResponse> items, ItemSearchPagination pagination) {}
