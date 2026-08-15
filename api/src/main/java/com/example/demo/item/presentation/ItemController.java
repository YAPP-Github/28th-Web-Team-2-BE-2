package com.example.demo.item.presentation;

import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.application.usecase.GetItemQueryUseCase;
import com.example.demo.item.presentation.converter.ItemQueryRequestConverter;
import com.example.demo.item.presentation.converter.ItemResultConverter;
import com.example.demo.item.presentation.dto.ItemPageResponse;
import com.example.demo.item.presentation.dto.ItemQueryRequest;
import com.example.demo.item.presentation.spec.ItemControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController implements ItemControllerSpec {

    private final GetItemQueryUseCase getItemQueryUseCase;
    private final ItemQueryRequestConverter itemQueryRequestConverter;
    private final ItemResultConverter itemResultConverter;

    @GetMapping
    @Override
    public ResponseEntity<ItemPageResponse> getItems(
        @Valid @ModelAttribute final ItemQueryRequest request) {
        final ItemQueryResult result = getItemQueryUseCase.execute(itemQueryRequestConverter.toQuery(request));
        final ItemPageResponse data = itemResultConverter.toResponse(result);
        return ResponseEntity.ok(data);
    }
}
