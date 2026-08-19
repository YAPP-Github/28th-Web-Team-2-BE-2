package com.example.demo.store.application.result;

import java.util.List;

public record RecommendedStoresResult(
        long totalCount,
        List<RecommendedStoreResult> stores) {}
