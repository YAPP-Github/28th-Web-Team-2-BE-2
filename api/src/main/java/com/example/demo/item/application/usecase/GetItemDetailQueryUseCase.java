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
import com.example.demo.item.domain.ItemUnit;
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
                onlineLowestPrice(item),
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
        if (priceGap == null || previousPrice == null || previousPrice.price() == 0) {
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

    /**
     * 온라인 최저가를 품목 기준 단위로 환산해 반환한다.
     *
     * <p>crawler 는 품목과 무관하게 100g 기준으로 저장하는데, 이 응답의 {@code latestLocalReportPrice}·
     * {@code todayPublicPrice}는 품목 기준 단위다. 화면은 단위를 한 번만 표시하고 그 아래 금액들이 모두 그 단위라고
     * 전제하므로, 환산하지 않으면 표시된 단위가 거짓이 된다.
     *
     * <p>{@code 1개}·{@code 1포기}처럼 무게로 환산할 수 없는 단위는 {@code null}이다. 이 응답에는 값마다 단위를 담을
     * 자리가 없어 100g 가격을 그대로 두면 다른 단위의 금액과 나란히 놓인다.
     */
    private Integer onlineLowestPrice(final Item item) {
        final ItemUnit itemUnit = ItemUnit.of(item.defaultUnit());
        if (!itemUnit.convertible()) {
            return null;
        }
        return onlinePriceQueryPort
                .findLowestPriceAtLatestCollectionDate(
                        item.id(), OnlinePriceCrawlResult.PER_100_GRAMS)
                .map(OnlinePrice::price)
                .map(price -> itemUnit.convert(price, OnlinePriceCrawlResult.PER_100_GRAMS))
                .orElse(null);
    }
}
