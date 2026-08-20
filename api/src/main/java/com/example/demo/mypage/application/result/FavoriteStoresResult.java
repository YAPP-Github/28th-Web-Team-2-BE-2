package com.example.demo.mypage.application.result;

import java.util.List;

public record FavoriteStoresResult(
        List<FavoriteStoreResult> stores,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {}
