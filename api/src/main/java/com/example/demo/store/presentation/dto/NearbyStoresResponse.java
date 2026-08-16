package com.example.demo.store.presentation.dto;

import java.util.List;

public record NearbyStoresResponse(long totalCount, List<NearbyStoreResponse> stores) {}
