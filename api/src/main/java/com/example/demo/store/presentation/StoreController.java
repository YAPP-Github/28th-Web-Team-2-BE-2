package com.example.demo.store.presentation;

import com.example.demo.store.application.usecase.GetNearbyStoresUseCase;
import com.example.demo.store.presentation.converter.StoreQueryConverter;
import com.example.demo.store.presentation.converter.StoreResultConverter;
import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import com.example.demo.store.presentation.dto.NearbyStoresResponse;
import com.example.demo.store.presentation.spec.StoreControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController implements StoreControllerSpec {

    private final GetNearbyStoresUseCase getNearbyStoresUseCase;
    private final StoreQueryConverter storeQueryConverter;
    private final StoreResultConverter storeResultConverter;

    @GetMapping("/nearby")
    @Override
    public ResponseEntity<NearbyStoresResponse> getNearbyStores(
            @Valid @ModelAttribute final NearbyStoreRequest request) {
        final NearbyStoresResponse response = storeResultConverter.toNearbyStoresResponse(
                getNearbyStoresUseCase.execute(storeQueryConverter.toNearbyStoreQuery(request)));
        return ResponseEntity.ok(response);
    }
}
