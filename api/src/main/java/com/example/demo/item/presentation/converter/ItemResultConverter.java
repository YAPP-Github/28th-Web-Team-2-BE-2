package com.example.demo.item.presentation.converter;

import com.example.demo.item.application.result.ItemPriceResult;
import com.example.demo.item.application.result.ItemDetailResult;
import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.presentation.dto.ItemPageResponse;
import com.example.demo.item.presentation.dto.ItemDetailResponse;
import com.example.demo.item.presentation.dto.ItemResponse;
import com.example.demo.item.presentation.dto.ItemSearchItemResponse;
import com.example.demo.item.presentation.dto.ItemSearchPagination;
import com.example.demo.item.presentation.dto.ItemSearchRequest;
import com.example.demo.item.presentation.dto.ItemSearchResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItemResultConverter {

    public ItemDetailResponse toResponse(final ItemDetailResult result) {
        return new ItemDetailResponse(
                result.itemId(),
                result.itemName(),
                result.itemImageUrl(),
                result.defaultUnit(),
                result.isLiked(),
                result.latestLocalReportPrice(),
                result.todayPublicPrice(),
                result.onlineLowestPrice(),
                result.baseDate(),
                result.priceGap(),
                result.priceDiffRate());
    }

    public ItemPageResponse toResponse(final ItemQueryResult result) {
        return new ItemPageResponse(
                result.baseDate(),
                result.totalCount(),
                result.categoryCounts(),
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.hasNext());
    }

    public ItemSearchResponse toSearchResponse(
            final ItemQueryResult result, final ItemSearchRequest request) {
        final List<ItemSearchItemResponse> items =
                result.items().stream().map(this::toSearchItemResponse).toList();
        final ItemSearchPagination pagination =
                new ItemSearchPagination(request.limit(), request.offset(), result.hasNext());
        return new ItemSearchResponse(result.totalCount(), items, pagination);
    }

    private ItemSearchItemResponse toSearchItemResponse(final ItemPriceResult result) {
        return new ItemSearchItemResponse(
                result.itemId(),
                result.itemName(),
                result.itemImageUrl(),
                result.price(),
                result.priceDiffRate());
    }

    private ItemResponse toResponse(final ItemPriceResult result) {
        return new ItemResponse(
                result.itemId(),
                result.itemName(),
                result.itemImageUrl(),
                result.defaultUnit(),
                result.price(),
                result.priceGap(),
                result.priceDiffRate(),
                result.isLiked());
    }
}
