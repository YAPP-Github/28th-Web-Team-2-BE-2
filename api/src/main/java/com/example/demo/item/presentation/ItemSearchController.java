package com.example.demo.item.presentation;

import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.application.usecase.GetItemQueryUseCase;
import com.example.demo.item.presentation.converter.ItemQueryRequestConverter;
import com.example.demo.item.presentation.converter.ItemResultConverter;
import com.example.demo.item.presentation.dto.ItemSearchRequest;
import com.example.demo.item.presentation.dto.ItemSearchResponse;
import com.example.demo.item.presentation.spec.ItemSearchControllerSpec;
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
public class ItemSearchController implements ItemSearchControllerSpec {

    private final GetItemQueryUseCase getItemQueryUseCase;
    private final ItemQueryRequestConverter itemQueryRequestConverter;
    private final ItemResultConverter itemResultConverter;

    @GetMapping("/search")
    @Override
    public ResponseEntity<ItemSearchResponse> searchItems(
            @Valid @ModelAttribute final ItemSearchRequest request) {
        final Long anonymousUserId = null; // 공개 검색이라 찜 여부를 반영하지 않는다
        final ItemQueryResult result = getItemQueryUseCase.execute(
                itemQueryRequestConverter.toSearchQuery(request), anonymousUserId);
        return ResponseEntity.ok(itemResultConverter.toSearchResponse(result, request));
    }
}
