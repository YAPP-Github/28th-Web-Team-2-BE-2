package com.example.demo.item.application.result;

import java.time.LocalDate;
import java.util.List;

/** 품목의 채널별 온라인 최저가다. 수집 데이터가 없으면 {@code onlinePrices}가 빈 목록이다. */
public record ItemOnlinePriceResult(Long itemId, List<ChannelPrice> onlinePrices) {

    public record ChannelPrice(
            Integer channelId,
            String channelName,
            String productName,
            Integer price,
            Integer quantity,
            String unit,
            Integer normalizedPrice,
            String deliveryNote,
            String productUrl,
            LocalDate collectedAt) {}
}
