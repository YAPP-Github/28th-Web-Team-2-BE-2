package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.AuthenticationRequiredException;
import com.example.demo.item.application.port.ItemQueryPort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.application.result.ItemPriceResult;
import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.domain.PublicPrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetItemQueryUseCase {

    private final ItemQueryPort itemQueryPort;
    private final PublicPriceQueryPort publicPriceQueryPort;

    @Transactional(readOnly = true)
    public ItemQueryResult execute(final ItemQuery query, final Long userId) {
        validateFavoriteOnly(query, userId);
        final LocalDate baseDate = publicPriceQueryPort.findLatestPriceDateByRegionId(query.regionId());
        final Page<Item> itemPage = itemQueryPort.findAll(query, userId);
        final Map<String, Long> categoryCounts = categoryCounts(itemQueryPort.countByCategory());
        final List<Long> itemIds = itemPage.getContent().stream().map(Item::id).toList();
        final List<PublicPrice> prices = findPrices(itemIds, query.regionId());
        final Set<Long> favoriteItemIds = findFavoriteItemIds(userId, itemIds);
        final Map<Long, PublicPrice> pricesByItemId = currentPrices(prices);
        final Map<Long, PublicPrice> previousPricesByItemId = previousPrices(prices, pricesByItemId);
        final List<ItemPriceResult> items = itemPage.getContent().stream()
                .map(item -> toResult(
                        item,
                        pricesByItemId.get(item.id()),
                        previousPricesByItemId.get(item.id()))
                        .withLiked(favoriteItemIds.contains(item.id())))
                .toList();
        final long totalCount = itemPage.getTotalElements();
        return new ItemQueryResult(
                baseDate,
                totalCount,
                categoryCounts,
                items,
                query.page(),
                query.size(),
                query.effectiveOffset() + items.size() < totalCount);
    }

    private Map<String, Long> categoryCounts(final Map<ItemCategory, Long> counts) {
        final Map<String, Long> result = new LinkedHashMap<>();
        for (final ItemCategory category : ItemCategory.values()) {
            result.put(category.code(), counts.getOrDefault(category, 0L));
        }
        return result;
    }

    private void validateFavoriteOnly(final ItemQuery query, final Long userId) {
        if (query.favoriteOnly() && userId == null) {
            throw new AuthenticationRequiredException();
        }
    }

    private List<PublicPrice> findPrices(final List<Long> itemIds, final String regionId) {
        if (itemIds.isEmpty()) {
            return List.of();
        }
        return publicPriceQueryPort.findByItemIdsAndRegionId(itemIds, regionId);
    }

    private Set<Long> findFavoriteItemIds(final Long userId, final List<Long> itemIds) {
        if (userId == null || itemIds.isEmpty()) {
            return Set.of();
        }
        return itemQueryPort.findFavoriteItemIds(userId, itemIds);
    }

    private Map<Long, PublicPrice> currentPrices(final List<PublicPrice> prices) {
        final Map<Long, PublicPrice> pricesByItemId = new HashMap<>();
        prices.forEach(price -> pricesByItemId.putIfAbsent(price.itemId(), price));
        return pricesByItemId;
    }

    private Map<Long, PublicPrice> previousPrices(
            final List<PublicPrice> prices, final Map<Long, PublicPrice> currentPrices) {
        final Map<Long, PublicPrice> previousPricesByItemId = new HashMap<>();
        prices.forEach(price -> {
            final PublicPrice currentPrice = currentPrices.get(price.itemId());
            if (!price.id().equals(currentPrice.id())
                    && !price.priceDate().equals(currentPrice.priceDate())) {
                previousPricesByItemId.putIfAbsent(price.itemId(), price);
            }
        });
        return previousPricesByItemId;
    }

    private ItemPriceResult toResult(
            final Item item, final PublicPrice publicPrice, final PublicPrice previousPrice) {
        if (publicPrice == null) {
            return new ItemPriceResult(
                    item.id(), item.name(), item.imageUrl(), item.defaultUnit(), null, null, null, false);
        }
        if (previousPrice == null) {
            return new ItemPriceResult(
                    item.id(),
                    item.name(),
                    item.imageUrl(),
                    item.defaultUnit(),
                    publicPrice.price(),
                    null,
                    null,
                    false);
        }
        return new ItemPriceResult(
                item.id(),
                item.name(),
                item.imageUrl(),
                item.defaultUnit(),
                publicPrice.price(),
                publicPrice.price() - previousPrice.price(),
                calculatePriceDiffRate(publicPrice, previousPrice),
                false);
    }

    private BigDecimal calculatePriceDiffRate(
            final PublicPrice publicPrice, final PublicPrice previousPrice) {
        if (previousPrice.price() == 0) {
            return null;
        }
        final BigDecimal priceGap = BigDecimal.valueOf(publicPrice.price())
                .subtract(BigDecimal.valueOf(previousPrice.price()));
        return priceGap
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousPrice.price()), 1, RoundingMode.HALF_UP);
    }
}
