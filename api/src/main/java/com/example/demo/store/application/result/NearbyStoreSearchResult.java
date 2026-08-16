package com.example.demo.store.application.result;

import java.util.List;

public record NearbyStoreSearchResult(long totalCount, List<NearbyStoreResult> stores) {}
