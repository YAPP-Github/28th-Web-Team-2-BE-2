package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.port.OnlineChannelQueryPort;
import com.example.demo.item.application.port.OnlineItemQueryPort;
import com.example.demo.item.application.port.OnlinePriceCrawlerPort;
import com.example.demo.item.application.result.OnlinePriceCollectionResult;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectOnlinePriceUseCase {

    private static final int MAX_RESULT_COUNT = 5;

    private final OnlineItemQueryPort onlineItemQueryPort;
    private final OnlineChannelQueryPort onlineChannelQueryPort;
    private final List<OnlinePriceCrawlerPort> crawlers;
    private final ReplaceOnlinePriceUseCase replaceOnlinePriceUseCase;

    public OnlinePriceCollectionResult execute(final LocalDate collectionDate) {
        Objects.requireNonNull(collectionDate, "collectionDate must not be null");
        int totalTaskCount = 0;
        int succeededTaskCount = 0;
        int failedTaskCount = 0;
        int savedPriceCount = 0;
        final List<Item> items = onlineItemQueryPort.findAll();
        final List<OnlineChannel> channels = onlineChannelQueryPort.findAll();
        for (Item item : items) {
            for (OnlineChannel channel : channels) {
                totalTaskCount++;
                try {
                    savedPriceCount += collect(item, channel, collectionDate);
                    succeededTaskCount++;
                } catch (RuntimeException exception) {
                    failedTaskCount++;
                    log.error(
                            "온라인 가격 수집에 실패했습니다. itemId={}, channelId={}, collectionDate={}",
                            item.id(), channel.id(), collectionDate, exception);
                }
            }
        }
        return new OnlinePriceCollectionResult(
                totalTaskCount, succeededTaskCount, failedTaskCount, savedPriceCount);
    }

    private int collect(
            final Item item,
            final OnlineChannel channel,
            final LocalDate collectionDate) {
        final OnlinePriceCrawlerPort crawler = crawler(channel.name());
        final List<OnlinePrice> prices = crawler.crawl(new CrawlOnlinePriceCommand(item.name())).stream()
                .filter(this::isValid)
                .limit(MAX_RESULT_COUNT)
                .map(result -> toOnlinePrice(item, channel, collectionDate, result))
                .toList();
        replaceOnlinePriceUseCase.execute(item.id(), channel.id(), collectionDate, prices);
        return prices.size();
    }

    private OnlinePriceCrawlerPort crawler(final String channelName) {
        return crawlers.stream()
                .filter(crawler -> crawler.channelName().equals(channelName))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ErrorType.CONFIGURATION_ERROR.description(),
                        ErrorType.CONFIGURATION_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private boolean isValid(final OnlinePriceCrawlResult result) {
        return result != null
                && result.price() != null
                && result.price().signum() > 0
                && result.unit() > 0
                && result.productName() != null
                && !result.productName().isBlank()
                && result.productUrl() != null;
    }

    private OnlinePrice toOnlinePrice(
            final Item item,
            final OnlineChannel channel,
            final LocalDate collectionDate,
            final OnlinePriceCrawlResult result) {
        return new OnlinePrice(
                item.id(),
                channel.id(),
                item.name(),
                result.productName(),
                result.price().setScale(0, RoundingMode.HALF_UP).intValueExact(),
                result.unit(),
                result.productUrl().toString(),
                result.deliveryNote(),
                collectionDate);
    }
}
