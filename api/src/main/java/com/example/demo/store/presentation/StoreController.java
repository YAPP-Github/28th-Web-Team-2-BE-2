package com.example.demo.store.presentation;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.common.security.JwtAuthenticationFilter;
import com.example.demo.store.application.usecase.GetNearbyStoresUseCase;
import com.example.demo.store.application.usecase.GetStoreDetailUseCase;
import com.example.demo.store.presentation.converter.StoreQueryConverter;
import com.example.demo.store.presentation.converter.StoreResultConverter;
import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import com.example.demo.store.presentation.dto.StoreDetailRequest;
import com.example.demo.store.presentation.dto.StoreDetailResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController implements StoreControllerSpec {

    private final GetNearbyStoresUseCase getNearbyStoresUseCase;
    private final GetStoreDetailUseCase getStoreDetailUseCase;
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

    @GetMapping("/{storeId}")
    @Override
    public ResponseEntity<StoreDetailResponse> getStoreDetail(
            @Positive @PathVariable final Long storeId,
            @Valid @ModelAttribute final StoreDetailRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) final AuthPrincipal principal,
            final Authentication authentication,
            final HttpServletRequest servletRequest) {
        rejectInvalidToken(servletRequest);
        final StoreDetailResponse response = storeResultConverter.toStoreDetailResponse(
                getStoreDetailUseCase.execute(
                        storeQueryConverter.toStoreDetailQuery(storeId, request, principal, authentication)));
        return ResponseEntity.ok(response);
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
