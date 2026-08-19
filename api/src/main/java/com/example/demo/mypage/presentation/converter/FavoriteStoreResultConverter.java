package com.example.demo.mypage.presentation.converter;

import com.example.demo.mypage.application.result.FavoriteStoreResult;
import com.example.demo.mypage.application.result.FavoriteStoresResult;
import com.example.demo.mypage.presentation.dto.FavoriteStoreResponse;
import com.example.demo.mypage.presentation.dto.FavoriteStoresResponse;
import org.springframework.stereotype.Component;

@Component
public class FavoriteStoreResultConverter {

    public FavoriteStoresResponse toResponse(final FavoriteStoresResult result) {
        return new FavoriteStoresResponse(
                result.stores().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext());
    }

    private FavoriteStoreResponse toResponse(final FavoriteStoreResult result) {
        return new FavoriteStoreResponse(
                result.storeId(),
                result.storeName(),
                result.storeImageUrl(),
                result.distanceMeters(),
                result.openStatus(),
                result.todayBusinessHours(),
                result.isLiked());
    }
}
