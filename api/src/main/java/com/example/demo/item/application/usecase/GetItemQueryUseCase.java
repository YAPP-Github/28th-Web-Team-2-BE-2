package com.example.demo.item.application.usecase;

import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.application.port.ItemQueryPort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.application.result.ItemPriceResult;
import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.PublicPrice;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetItemQueryUseCase {

    private final ItemQueryPort itemQueryPort;
    private final PublicPriceQueryPort publicPriceQueryPort;

    @Transactional(readOnly = true)
    public ItemQueryResult execute(final ItemQuery query) {
        final Page<Item> itemPage = itemQueryPort.findAll(PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.ASC, "id")));
        final LocalDate baseDate = publicPriceQueryPort.findLatestPriceDateByRegionId(query.regionId());
        final List<Long> itemIds = itemPage.getContent().stream().map(Item::id).toList();
        final PriceHistory priceHistory = findPrices(itemIds, query.regionId());
        final List<ItemPriceResult> items = itemPage.getContent().stream()
                .map(item -> toResult(
                        item,
                        priceHistory.currentPrices().get(item.id()),
                        priceHistory.previousPrices().get(item.id())))
                .toList();
        return new ItemQueryResult(
                baseDate,
                itemPage.getTotalElements(),
                items,
                query.page(),
                query.size(),
                itemPage.hasNext());
    }

    private PriceHistory findPrices(final List<Long> itemIds, final String regionId) {
        if (itemIds.isEmpty()) {
            return new PriceHistory(Map.of(), Map.of());
        }
        final Map<Long, PublicPrice> pricesByItemId = new HashMap<>();
        final Map<Long, PublicPrice> previousPricesByItemId = new HashMap<>();
        // ponytail: loads history for the page once; use a per-item top-2 query if history volume matters.
        for (final PublicPrice price : publicPriceQueryPort.findByItemIdsAndRegionId(itemIds, regionId)) {
            if (!pricesByItemId.containsKey(price.itemId())) {
                pricesByItemId.put(price.itemId(), price);
                continue;
            }
            if (previousPricesByItemId.containsKey(price.itemId())) {
                continue;
            }
            if (price.priceDate().equals(pricesByItemId.get(price.itemId()).priceDate())) {
                continue;
            }
            previousPricesByItemId.putIfAbsent(price.itemId(), price);
        }
        return new PriceHistory(pricesByItemId, previousPricesByItemId);
    }

    private ItemPriceResult toResult(
            final Item item, final PublicPrice publicPrice, final PublicPrice previousPrice) {
        if (publicPrice == null) {
            return new ItemPriceResult(item.id(), item.name(), item.imageUrl(), null, null);
        }
        if (previousPrice == null) {
            return new ItemPriceResult(item.id(), item.name(), item.imageUrl(), publicPrice.price(), null);
        }
        return new ItemPriceResult(
                item.id(),
                item.name(),
                item.imageUrl(),
                publicPrice.price(),
                publicPrice.price() - previousPrice.price());
    }

    private record PriceHistory(
            Map<Long, PublicPrice> currentPrices, Map<Long, PublicPrice> previousPrices) {}
}
