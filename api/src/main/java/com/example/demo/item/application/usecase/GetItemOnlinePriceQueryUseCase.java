package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.application.port.OnlineChannelQueryPort;
import com.example.demo.item.application.port.OnlinePriceQueryPort;
import com.example.demo.item.application.result.ItemOnlinePriceResult;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 품목의 채널별 온라인 최저가를 조회한다.
 *
 * <p>크롤러 4개가 모두 100g 기준으로 환산해 저장하므로({@link OnlinePriceCrawlResult#PER_100_GRAMS}) 비교 대상은 100g
 * 기준 행뿐이다. 따라서 {@code quantity}는 기준 단위 1배수(=100g), {@code normalizedPrice}는 저장된 가격 그대로다. 다른
 * 기준으로 저장된 행은 채널 간 비교가 성립하지 않아 제외한다.
 */
@Service
@RequiredArgsConstructor
public class GetItemOnlinePriceQueryUseCase {

    private static final int NORMALIZED_UNIT = OnlinePriceCrawlResult.PER_100_GRAMS;
    private static final int NORMALIZED_QUANTITY = 1;
    private static final String NORMALIZED_UNIT_TYPE = "g";

    private final ItemExistencePort itemExistencePort;
    private final OnlineChannelQueryPort onlineChannelQueryPort;
    private final OnlinePriceQueryPort onlinePriceQueryPort;

    @Transactional(readOnly = true)
    public ItemOnlinePriceResult execute(final Long itemId) {
        final Item item = findItem(itemId);
        return new ItemOnlinePriceResult(item.id(), findChannelPrices(item.id()));
    }

    private Item findItem(final Long itemId) {
        return itemExistencePort.findById(itemId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
    }

    private List<ItemOnlinePriceResult.ChannelPrice> findChannelPrices(final Long itemId) {
        final LocalDate collectionDate = latestCollectionDate(itemId);
        if (collectionDate == null) {
            return List.of();
        }
        final Map<Integer, String> channelNames = channelNames();
        return channelNames.keySet().stream()
                .sorted()
                .map(channelId -> lowestPrice(itemId, channelId, collectionDate))
                .filter(Objects::nonNull)
                .map(price -> toChannelPrice(price, channelNames.get(price.channelId())))
                .toList();
    }

    /** 가장 최근 수집 회차만 비교한다. 회차가 섞이면 채널 간 비교가 성립하지 않는다. */
    private LocalDate latestCollectionDate(final Long itemId) {
        return onlinePriceQueryPort
                .findLowestPriceAtLatestCollectionDate(itemId, NORMALIZED_UNIT)
                .map(OnlinePrice::createdAt)
                .orElse(null);
    }

    private Map<Integer, String> channelNames() {
        return onlineChannelQueryPort.findAll().stream()
                .collect(Collectors.toMap(
                        OnlineChannel::id, OnlineChannel::name, (left, right) -> left));
    }

    private OnlinePrice lowestPrice(
            final Long itemId, final Integer channelId, final LocalDate collectionDate) {
        return onlinePriceQueryPort
                .findLowestPrice(itemId, channelId, collectionDate, NORMALIZED_UNIT)
                .orElse(null);
    }

    private ItemOnlinePriceResult.ChannelPrice toChannelPrice(
            final OnlinePrice price, final String channelName) {
        return new ItemOnlinePriceResult.ChannelPrice(
                price.channelId(),
                channelName,
                price.productName(),
                price.price(),
                NORMALIZED_QUANTITY,
                NORMALIZED_UNIT_TYPE,
                price.price(),
                price.deliveryNote(),
                price.productUrl(),
                price.createdAt());
    }
}
