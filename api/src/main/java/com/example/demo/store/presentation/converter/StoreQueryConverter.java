package com.example.demo.store.presentation.converter;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.query.StoreDetailQuery;
import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import com.example.demo.store.presentation.dto.StoreDetailRequest;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StoreQueryConverter {

    public NearbyStoreQuery toNearbyStoreQuery(
            final NearbyStoreRequest request,
            final AuthPrincipal principal,
            final Authentication authentication) {
        final boolean roleUser = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> Objects.equals("ROLE_USER", authority.getAuthority()));
        final Long userId = roleUser && principal != null ? principal.userId() : null;
        return new NearbyStoreQuery(
                request.latitude(),
                request.longitude(),
                request.radius(),
                request.onlyLiked(),
                roleUser,
                userId,
                request.keyword());
    }

    public StoreDetailQuery toStoreDetailQuery(
            final Long storeId,
            final StoreDetailRequest request,
            final AuthPrincipal principal,
            final Authentication authentication) {
        return new StoreDetailQuery(
                storeId,
                request.latitude(),
                request.longitude(),
                userId(principal, authentication));
    }

    private Long userId(final AuthPrincipal principal, final Authentication authentication) {
        final boolean roleUser = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> Objects.equals("ROLE_USER", authority.getAuthority()));
        return roleUser && principal != null ? principal.userId() : null;
    }
}
