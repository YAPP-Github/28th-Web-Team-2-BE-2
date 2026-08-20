package com.example.demo.item.application.result;

import java.time.LocalDate;
import java.util.List;

/** 품목의 채널별 온라인 최저가다. 수집 데이터가 없으면 {@code onlinePrices}가 빈 목록이다. */
public record ItemOnlinePriceResult(Long itemId, List<ChannelPrice> onlinePrices) {

    /**
     * 채널 하나의 최저가다.
     *
     * <p>{@code price}는 {@code unit} 기준 가격이다. 품목 기준 단위로 환산할 수 있으면 그 단위이고, 무게가 아닌 단위
     * ({@code 1개}·{@code 1포기})는 환산할 수 없어 수집 기준인 {@code 100g}을 그대로 쓴다. 어느 쪽이든 {@code unit}이
     * 실제 기준을 말한다.
     */
    public record ChannelPrice(
            Integer channelId,
            String channelName,
            String channelKind,
            String productName,
            Integer price,
            String unit,
            String deliveryNote,
            String productUrl,
            LocalDate collectedAt) {}
}
