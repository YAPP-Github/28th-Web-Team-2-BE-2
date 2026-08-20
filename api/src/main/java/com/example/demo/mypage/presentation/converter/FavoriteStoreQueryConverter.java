package com.example.demo.mypage.presentation.converter;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.mypage.application.query.FavoriteStoresQuery;
import com.example.demo.mypage.presentation.dto.FavoriteStoresRequest;
import org.springframework.stereotype.Component;

@Component
public class FavoriteStoreQueryConverter {

    public FavoriteStoresQuery toQuery(
            final FavoriteStoresRequest request, final AuthPrincipal principal) {
        return new FavoriteStoresQuery(
                principal.userId(),
                request.latitude(),
                request.longitude(),
                request.page(),
                request.size());
    }
}
