package com.example.demo.item.presentation;

import com.example.demo.common.presentation.DirectResponse;
import com.example.demo.item.application.result.ItemOnlinePriceResult;
import com.example.demo.item.application.result.ItemPublicPriceResult;
import com.example.demo.item.application.usecase.GetItemOnlinePriceQueryUseCase;
import com.example.demo.item.application.usecase.GetItemPublicPriceQueryUseCase;
import com.example.demo.item.presentation.converter.ItemQueryRequestConverter;
import com.example.demo.item.presentation.converter.ItemResultConverter;
import com.example.demo.item.presentation.dto.ItemOnlinePriceResponse;
import com.example.demo.item.presentation.dto.ItemPublicPriceRequest;
import com.example.demo.item.presentation.dto.ItemPublicPriceResponse;
import com.example.demo.item.presentation.spec.ItemPriceControllerSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items/{itemId}")
@RequiredArgsConstructor
public class ItemPriceController implements ItemPriceControllerSpec {

    private final GetItemPublicPriceQueryUseCase getItemPublicPriceQueryUseCase;
    private final GetItemOnlinePriceQueryUseCase getItemOnlinePriceQueryUseCase;
    private final ItemQueryRequestConverter itemQueryRequestConverter;
    private final ItemResultConverter itemResultConverter;

    @DirectResponse
    @GetMapping("/public-prices")
    @Override
    public ResponseEntity<ItemPublicPriceResponse> getPublicPrices(
            @Positive @PathVariable final Long itemId,
            @Valid @ModelAttribute final ItemPublicPriceRequest request) {
        final ItemPublicPriceResult result =
                getItemPublicPriceQueryUseCase.execute(itemQueryRequestConverter.toQuery(itemId, request));
        return ResponseEntity.ok(itemResultConverter.toResponse(result));
    }

    @DirectResponse
    @GetMapping("/online-prices")
    @Override
    public ResponseEntity<ItemOnlinePriceResponse> getOnlinePrices(
            @Positive @PathVariable final Long itemId) {
        final ItemOnlinePriceResult result = getItemOnlinePriceQueryUseCase.execute(itemId);
        return ResponseEntity.ok(itemResultConverter.toResponse(result));
    }
}
