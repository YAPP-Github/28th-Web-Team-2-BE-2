package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.application.port.OnlineChannelQueryPort;
import com.example.demo.item.application.port.OnlinePriceQueryPort;
import com.example.demo.item.application.result.ItemOnlinePriceResult;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemUnit;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 품목의 채널별 온라인 최저가를 조회한다.
 *
 * <p>crawler 4개가 모두 100g 기준으로 환산해 저장하므로({@link OnlinePriceCrawlResult#PER_100_GRAMS}) 비교 대상은
 * 100g 기준 행뿐이다. 다른 기준으로 저장된 행은 채널 간 비교가 성립하지 않아 제외한다.
 *
 * <p>응답은 품목 기준 단위로 환산해 내려준다. 품목 상세 화면이 단위를 한 번만 표시하고 그 아래 금액들이 모두 그 단위 기준이기
 * 때문이다. {@code 1개}·{@code 1포기}처럼 무게로 환산할 수 없는 단위는 수집 기준인 100g을 그대로 쓰고 {@code unit}에
 * 그 사실을 담는다.
 */
@Service
@RequiredArgsConstructor
public class GetItemOnlinePriceQueryUseCase {

    private static final int NORMALIZED_UNIT = OnlinePriceCrawlResult.PER_100_GRAMS;
    private static final String NORMALIZED_UNIT_LABEL = "100g";
    private static final Set<String> SUPPORTED_CHANNEL_NAMES = Set.of("오아시스", "컬리", "11번가", "GS SHOP");

    private final ItemExistencePort itemExistencePort;
    private final OnlineChannelQueryPort onlineChannelQueryPort;
    private final OnlinePriceQueryPort onlinePriceQueryPort;

    @Transactional(readOnly = true)
    public ItemOnlinePriceResult execute(final Long itemId) {
        final Item item = findItem(itemId);
        return new ItemOnlinePriceResult(
                item.id(), findChannelPrices(item.id(), ItemUnit.of(item.defaultUnit())));
    }

    private Item findItem(final Long itemId) {
        return itemExistencePort.findById(itemId).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
    }

    private List<ItemOnlinePriceResult.ChannelPrice> findChannelPrices(
            final Long itemId, final ItemUnit itemUnit) {
        final Map<Integer, OnlineChannel> channels = channels();
        final LocalDate collectionDate = latestCollectionDate(itemId, channels.keySet());
        if (collectionDate == null) {
            return List.of();
        }
        return channels.keySet().stream()
                .sorted()
                .map(channelId -> lowestPrice(itemId, channelId, collectionDate))
                .filter(Objects::nonNull)
                .map(price -> toChannelPrice(price, channels.get(price.channelId()), itemUnit))
                .toList();
    }

    /** 가장 최근 수집 회차만 비교한다. 회차가 섞이면 채널 간 비교가 성립하지 않는다. */
    private LocalDate latestCollectionDate(final Long itemId, final Collection<Integer> channelIds) {
        if (channelIds.isEmpty()) {
            return null;
        }
        return onlinePriceQueryPort
                .findLatestCollectionDate(itemId, NORMALIZED_UNIT, channelIds)
                .orElse(null);
    }

    private Map<Integer, OnlineChannel> channels() {
        return onlineChannelQueryPort.findAll().stream()
                .filter(channel -> SUPPORTED_CHANNEL_NAMES.contains(channel.name()))
                .collect(Collectors.toMap(
                        OnlineChannel::id, channel -> channel, (left, right) -> left));
    }

    private OnlinePrice lowestPrice(
            final Long itemId, final Integer channelId, final LocalDate collectionDate) {
        return onlinePriceQueryPort
                .findLowestPrice(itemId, channelId, collectionDate, NORMALIZED_UNIT)
                .orElse(null);
    }

    private ItemOnlinePriceResult.ChannelPrice toChannelPrice(
            final OnlinePrice price, final OnlineChannel channel, final ItemUnit itemUnit) {
        return new ItemOnlinePriceResult.ChannelPrice(
                price.channelId(),
                channel.name(),
                channel.kind(),
                price.productName(),
                priceIn(itemUnit, price.price()),
                unitLabel(itemUnit),
                price.deliveryNote(),
                price.productUrl(),
                price.createdAt());
    }

    /** 품목 기준 단위로 환산한 가격이다. 환산할 수 없으면 수집 기준(100g) 가격을 그대로 쓴다. */
    private Integer priceIn(final ItemUnit itemUnit, final Integer price) {
        if (!itemUnit.convertible()) {
            return price;
        }
        return itemUnit.convert(price, NORMALIZED_UNIT);
    }

    private String unitLabel(final ItemUnit itemUnit) {
        if (!itemUnit.convertible()) {
            return NORMALIZED_UNIT_LABEL;
        }
        return itemUnit.label();
    }
}
