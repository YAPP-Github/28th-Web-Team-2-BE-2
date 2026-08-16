package com.example.demo.store.application.result;

import java.util.List;

public record NearbyStoresResult(long totalCount, List<NearbyStoreResult> stores) {}
