package com.example.demo.item.presentation.converter;

import com.example.demo.item.application.result.ItemPriceResult;
import com.example.demo.item.application.result.ItemDetailResult;
import com.example.demo.item.application.result.ItemOnlinePriceResult;
import com.example.demo.item.application.result.ItemPublicPriceResult;
import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.presentation.dto.ItemPageResponse;
import com.example.demo.item.presentation.dto.ItemDetailResponse;
import com.example.demo.item.presentation.dto.ItemOnlinePriceResponse;
import com.example.demo.item.presentation.dto.ItemPublicPriceResponse;
import com.example.demo.item.presentation.dto.OnlineChannelPriceResponse;
import com.example.demo.item.presentation.dto.ItemResponse;
import com.example.demo.item.presentation.dto.PublicPricePointResponse;
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

    public ItemOnlinePriceResponse toResponse(final ItemOnlinePriceResult result) {
        final List<OnlineChannelPriceResponse> onlinePrices =
                result.onlinePrices().stream().map(this::toResponse).toList();
        return new ItemOnlinePriceResponse(result.itemId(), onlinePrices);
    }

    private OnlineChannelPriceResponse toResponse(
            final ItemOnlinePriceResult.ChannelPrice result) {
        return new OnlineChannelPriceResponse(
                result.channelId(),
                result.channelName(),
                result.productName(),
                result.price(),
                result.quantity(),
                result.unit(),
                result.normalizedPrice(),
                result.deliveryNote(),
                result.productUrl(),
                result.collectedAt());
    }

    public ItemPublicPriceResponse toResponse(final ItemPublicPriceResult result) {
        final List<PublicPricePointResponse> points =
                result.points().stream().map(this::toResponse).toList();
        return new ItemPublicPriceResponse(
                result.itemId(), result.defaultUnit(), result.period(), points);
    }

    private PublicPricePointResponse toResponse(final ItemPublicPriceResult.Point result) {
        return new PublicPricePointResponse(result.date(), result.price());
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
