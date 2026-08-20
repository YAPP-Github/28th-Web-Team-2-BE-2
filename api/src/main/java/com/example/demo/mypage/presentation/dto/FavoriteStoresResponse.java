package com.example.demo.mypage.presentation.dto;

import java.util.List;

public record FavoriteStoresResponse(
        List<FavoriteStoreResponse> stores,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {}
