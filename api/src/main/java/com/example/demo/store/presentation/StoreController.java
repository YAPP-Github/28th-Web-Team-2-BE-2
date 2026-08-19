package com.example.demo.store.presentation;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.common.security.JwtAuthenticationFilter;
import com.example.demo.store.application.usecase.GetNearbyStoresUseCase;
import com.example.demo.store.application.usecase.GetRecommendedStoresUseCase;
import com.example.demo.store.application.usecase.StoreFavoriteUseCase;
import com.example.demo.store.presentation.converter.StoreCommandConverter;
import com.example.demo.store.presentation.converter.StoreQueryConverter;
import com.example.demo.store.presentation.converter.StoreResultConverter;
import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import com.example.demo.store.presentation.dto.RecommendedStoreRequest;
import com.example.demo.store.presentation.dto.RecommendedStoresResponse;
import com.example.demo.store.presentation.spec.StoreControllerSpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController implements StoreControllerSpec {

    private final GetNearbyStoresUseCase getNearbyStoresUseCase;
    private final GetRecommendedStoresUseCase getRecommendedStoresUseCase;
    private final StoreFavoriteUseCase storeFavoriteUseCase;
    private final StoreCommandConverter storeCommandConverter;
    private final StoreQueryConverter storeQueryConverter;
    private final StoreResultConverter storeResultConverter;

    @GetMapping("/nearby")
    @Override
    public ResponseEntity<NearbyStoresResponse> getNearbyStores(
            @Valid @ModelAttribute final NearbyStoreRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) final AuthPrincipal principal,
            final Authentication authentication,
            final HttpServletRequest servletRequest) {
        rejectInvalidToken(servletRequest);
        final NearbyStoresResponse response = storeResultConverter.toNearbyStoresResponse(
                getNearbyStoresUseCase.execute(
                        storeQueryConverter.toNearbyStoreQuery(request, principal, authentication)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendation")
    @Override
    public ResponseEntity<RecommendedStoresResponse> getRecommendedStores(
            @Valid @ModelAttribute final RecommendedStoreRequest request,
            final HttpServletRequest servletRequest) {
        rejectInvalidToken(servletRequest);
        final RecommendedStoresResponse response = storeResultConverter.toRecommendedStoresResponse(
                getRecommendedStoresUseCase.execute(
                        storeQueryConverter.toRecommendedStoreQuery(request)));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{storeId}/favorite")
    @Override
    public ResponseEntity<Void> addFavorite(
            @Positive @PathVariable final Long storeId,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        storeFavoriteUseCase.add(storeCommandConverter.toStoreFavoriteCommand(storeId, principal));
        return ResponseEntity.noContent().build();
    }

    private void rejectInvalidToken(final HttpServletRequest request) {
        if (request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE)
                instanceof ErrorType errorType) {
            throw new ApiException(
                    errorType.description(),
                    errorType,
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
