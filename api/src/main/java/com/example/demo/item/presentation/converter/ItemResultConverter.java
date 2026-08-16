package com.example.demo.item.presentation.converter;

import com.example.demo.item.application.result.ItemPriceResult;
import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.presentation.dto.ItemPageResponse;
import com.example.demo.item.presentation.dto.ItemResponse;
import org.springframework.stereotype.Component;

@Component
public class ItemResultConverter {

    public ItemPageResponse toResponse(final ItemQueryResult result) {
        return new ItemPageResponse(
                result.baseDate(),
                result.totalCount(),
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.hasNext());
    }

    private ItemResponse toResponse(final ItemPriceResult result) {
        return new ItemResponse(
                result.itemId(),
                result.itemName(),
                result.itemImageUrl(),
                result.defaultUnit(),
                result.price(),
                result.priceGap(),
                result.priceDiffRate());
    }
}
