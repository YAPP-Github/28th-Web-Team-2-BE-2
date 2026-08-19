package com.example.demo.store.presentation.dto;

import java.util.List;

public record RecommendedStoresResponse(long totalCount, List<RecommendedStoreResponse> stores) {}
