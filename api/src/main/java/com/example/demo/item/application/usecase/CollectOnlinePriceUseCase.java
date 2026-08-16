package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.port.BatchJobPersistencePort;
import com.example.demo.item.application.port.OnlineChannelQueryPort;
import com.example.demo.item.application.port.OnlineItemQueryPort;
import com.example.demo.item.application.port.OnlinePriceCrawlerPort;
import com.example.demo.item.application.result.BatchJobStatus;
import com.example.demo.item.application.result.OnlinePriceCollectionResult;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectOnlinePriceUseCase {

    private static final int MAX_RESULT_COUNT = 5;
    private static final int FIRST_ATTEMPT = 1;
    private static final String JOB_NAME = "ONLINE_PRICE_COLLECTION";
    private static final String JOB_FAILURE_MESSAGE = "온라인 가격 job 실행에 실패했습니다.";

    private final OnlineItemQueryPort onlineItemQueryPort;
    private final OnlineChannelQueryPort onlineChannelQueryPort;
    private final List<OnlinePriceCrawlerPort> crawlers;
    private final ReplaceOnlinePriceUseCase replaceOnlinePriceUseCase;
    private final BatchJobPersistencePort batchJobPersistencePort;

    public OnlinePriceCollectionResult execute(final LocalDate collectionDate) {
        Objects.requireNonNull(collectionDate, "collectionDate must not be null");
        final Long jobExecutionId = batchJobPersistencePort.start(JOB_NAME);
        int totalTaskCount = 0;
        int succeededTaskCount = 0;
        int failedTaskCount = 0;
        int savedPriceCount = 0;
        try {
            final List<Item> items = onlineItemQueryPort.findAll();
            final List<OnlineChannel> channels = onlineChannelQueryPort.findAll();
            final List<OnlineChannel> runnableChannels = channels.stream()
                    .filter(this::hasCrawler)
                    .toList();
            final boolean hasSkippedChannel = runnableChannels.size() != channels.size();
            totalTaskCount = items.size() * runnableChannels.size();
            for (Item item : items) {
                for (OnlineChannel channel : runnableChannels) {
                    final List<OnlinePrice> prices;
                    try {
                        prices = crawl(item, channel, collectionDate);
                    } catch (RuntimeException exception) {
                        failedTaskCount++;
                        batchJobPersistencePort.recordItemError(
                                jobExecutionId,
                                item.id(),
                                channel.id(),
                                FIRST_ATTEMPT,
                                errorType(exception),
                                errorMessage(exception));
                        log.error(
                                "온라인 가격 수집에 실패했습니다. itemId={}, channelId={}, collectionDate={}",
                                item.id(), channel.id(), collectionDate, exception);
                        continue;
                    }
                    replaceOnlinePriceUseCase.execute(item.id(), channel.id(), collectionDate, prices);
                    savedPriceCount += prices.size();
                    succeededTaskCount++;
                }
            }
            final BatchJobStatus status = status(
                    totalTaskCount, succeededTaskCount, failedTaskCount, hasSkippedChannel);
            batchJobPersistencePort.finish(
                    jobExecutionId,
                    status,
                    totalTaskCount,
                    succeededTaskCount,
                    status == BatchJobStatus.FAILED ? JOB_FAILURE_MESSAGE : null);
            return new OnlinePriceCollectionResult(
                    totalTaskCount, succeededTaskCount, failedTaskCount, savedPriceCount);
        } catch (RuntimeException exception) {
            failJob(jobExecutionId, totalTaskCount, succeededTaskCount, exception);
            throw exception;
        }
    }

    private List<OnlinePrice> crawl(
            final Item item,
            final OnlineChannel channel,
            final LocalDate collectionDate) {
        final Set<String> productUrls = new HashSet<>();
        return crawler(channel.name()).orElseThrow().crawl(new CrawlOnlinePriceCommand(item.name())).stream()
                .filter(this::isValid)
                .filter(result -> productUrls.add(result.productUrl().toString()))
                .limit(MAX_RESULT_COUNT)
                .map(result -> toOnlinePrice(item, channel, collectionDate, result))
                .toList();
    }

    private boolean hasCrawler(final OnlineChannel channel) {
        return crawler(channel.name()).isPresent();
    }

    private Optional<OnlinePriceCrawlerPort> crawler(final String channelName) {
        return crawlers.stream()
                .filter(crawler -> crawler.channelName().equals(channelName))
                .findFirst();
    }

    private BatchJobStatus status(
            final int totalTaskCount,
            final int succeededTaskCount,
            final int failedTaskCount,
            final boolean hasSkippedChannel) {
        if (totalTaskCount == 0 || succeededTaskCount == 0) {
            return BatchJobStatus.FAILED;
        }
        if (failedTaskCount > 0 || hasSkippedChannel) {
            return BatchJobStatus.PARTIAL;
        }
        return BatchJobStatus.COMPLETED;
    }

    private String errorType(final RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.errorType().name();
        }
        return ErrorType.UNKNOWN_ERROR.name();
    }

    private String errorMessage(final RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.errorType().description();
        }
        return ErrorType.UNKNOWN_ERROR.description();
    }

    private void failJob(
            final Long jobExecutionId,
            final int totalTaskCount,
            final int succeededTaskCount,
            final RuntimeException cause) {
        try {
            batchJobPersistencePort.finish(
                    jobExecutionId,
                    BatchJobStatus.FAILED,
                    totalTaskCount,
                    succeededTaskCount,
                    JOB_FAILURE_MESSAGE);
        } catch (RuntimeException exception) {
            cause.addSuppressed(exception);
        }
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
