package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.application.port.ItemQueryPort;
import com.example.demo.item.application.port.OnlinePriceQueryPort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.application.query.ItemDetailQuery;
import com.example.demo.item.application.result.ItemDetailResult;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlinePrice;
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.report.application.port.UserReportQueryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetItemDetailQueryUseCase {

    private final ItemExistencePort itemExistencePort;
    private final ItemQueryPort itemQueryPort;
    private final PublicPriceQueryPort publicPriceQueryPort;
    private final UserReportQueryPort userReportQueryPort;
    private final OnlinePriceQueryPort onlinePriceQueryPort;

    @Transactional(readOnly = true)
    public ItemDetailResult execute(final ItemDetailQuery query, final Long userId) {
        final Item item = findItem(query.itemId());
        final List<PublicPrice> publicPrices =
                publicPriceQueryPort.findByItemIdAndRegionId(query.itemId(), query.regionId());
        final PublicPrice latestPrice = priceAt(publicPrices, 0);
        final PublicPrice previousPrice = priceAt(publicPrices, 1);
        final Integer priceGap = priceGap(latestPrice, previousPrice);
        return new ItemDetailResult(
                item.id(),
                item.name(),
                item.imageUrl(),
                item.defaultUnit(),
                isLiked(userId, item.id()),
                latestLocalReportPrice(item, query.regionId()),
                price(latestPrice),
                onlineLowestPrice(item.id()),
                publicPriceQueryPort.findLatestPriceDateByRegionId(query.regionId()),
                priceGap,
                priceDiffRate(priceGap, previousPrice));
    }

    private Item findItem(final Long itemId) {
        return itemExistencePort.findById(itemId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
    }

    private PublicPrice priceAt(final List<PublicPrice> prices, final int index) {
        if (prices.size() <= index) {
            return null;
        }
        return prices.get(index);
    }

    private Integer price(final PublicPrice price) {
        if (price == null) {
            return null;
        }
        return price.price();
    }

    private Integer priceGap(final PublicPrice latestPrice, final PublicPrice previousPrice) {
        if (latestPrice == null || previousPrice == null) {
            return null;
        }
        return latestPrice.price() - previousPrice.price();
    }

    private BigDecimal priceDiffRate(final Integer priceGap, final PublicPrice previousPrice) {
        if (priceGap == null || previousPrice.price() == 0) {
            return null;
        }
        return BigDecimal.valueOf(priceGap)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousPrice.price()), 1, RoundingMode.HALF_UP);
    }

    private boolean isLiked(final Long userId, final Long itemId) {
        if (userId == null) {
            return false;
        }
        return itemQueryPort.findFavoriteItemIds(userId, List.of(itemId)).contains(itemId);
    }

    private Integer latestLocalReportPrice(final Item item, final String regionId) {
        if (item.defaultUnit() == null) {
            return null;
        }
        return userReportQueryPort
                .findLatestPrice(item.id(), regionId, item.defaultUnit())
                .orElse(null);
    }

    private Integer onlineLowestPrice(final Long itemId) {
        return onlinePriceQueryPort
                .findLowestPriceAtLatestCollectionDate(
                        itemId, OnlinePriceCrawlResult.PER_100_GRAMS)
                .map(OnlinePrice::price)
                .orElse(null);
    }
}
