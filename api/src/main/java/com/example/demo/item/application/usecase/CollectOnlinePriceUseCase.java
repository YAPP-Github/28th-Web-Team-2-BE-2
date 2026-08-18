package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.contract.BatchItemFailure;
import com.example.demo.item.application.contract.BatchJobCompletion;
import com.example.demo.item.application.port.BatchExecutionOutcome;
import com.example.demo.item.application.port.BatchJobPersistencePort;
import com.example.demo.item.application.port.BatchMetricsPort;
import com.example.demo.item.application.port.BatchRetryDelayPort;
import com.example.demo.item.application.port.OnlineChannelQueryPort;
import com.example.demo.item.application.port.OnlineItemQueryPort;
import com.example.demo.item.application.port.OnlinePriceCrawlerPort;
import com.example.demo.item.application.result.BatchJobStatus;
import com.example.demo.item.application.result.OnlinePriceCollectionResult;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.io.IOException;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectOnlinePriceUseCase {

    private static final int MAX_RESULT_COUNT = 5;
    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final String JOB_NAME = "ONLINE_PRICE_COLLECTION";
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(2),
            Duration.ofMinutes(4),
            Duration.ofMinutes(8));

    private final OnlineItemQueryPort onlineItemQueryPort;
    private final OnlineChannelQueryPort onlineChannelQueryPort;
    private final List<OnlinePriceCrawlerPort> crawlers;
    private final ReplaceOnlinePriceUseCase replaceOnlinePriceUseCase;
    private final BatchJobPersistencePort batchJobPersistencePort;
    private final BatchRetryDelayPort retryDelayPort;
    private final BatchMetricsPort metricsPort;

    public OnlinePriceCollectionResult execute(final LocalDate collectionDate) {
        Objects.requireNonNull(collectionDate, "collectionDate must not be null");
        final long jobStartedAt = System.nanoTime();
        final Long jobExecutionId = batchJobPersistencePort.start(JOB_NAME);
        log.info("batch job started job={} executionId={}", JOB_NAME, jobExecutionId);
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
            channels.stream()
                    .filter(channel -> !hasCrawler(channel))
                    .forEach(channel -> metricsPort.recordExecution(
                            JOB_NAME, channel.name(), BatchExecutionOutcome.SKIP));
            final boolean hasSkippedChannel = runnableChannels.size() != channels.size();
            totalTaskCount = items.size() * runnableChannels.size();
            for (Item item : items) {
                for (OnlineChannel channel : runnableChannels) {
                    final long taskStartedAt = System.nanoTime();
                    int attemptCount = 1;
                    try {
                        final CrawlExecution execution = crawlWithRetry(item, channel, collectionDate);
                        attemptCount = execution.attemptCount();
                        replaceOnlinePriceUseCase.execute(
                                item.id(), channel.id(), collectionDate, execution.prices());
                        savedPriceCount += execution.prices().size();
                        succeededTaskCount++;
                        metricsPort.recordExecution(
                                JOB_NAME, channel.name(), BatchExecutionOutcome.SUCCESS);
                    } catch (CrawlFailure exception) {
                        attemptCount = exception.attemptCount();
                        failedTaskCount++;
                        metricsPort.recordExecution(
                                JOB_NAME, channel.name(), BatchExecutionOutcome.FAILURE);
                        log.error(
                                "batch task failed job={} itemId={} channel={} attempt={} errorType={} durationMs={}",
                                JOB_NAME,
                                item.id(),
                                channel.name(),
                                attemptCount,
                                errorType(exception.finalCause()),
                                elapsedMillis(taskStartedAt));
                        batchJobPersistencePort.recordItemError(
                                jobExecutionId,
                                new BatchItemFailure(
                                        item.id(),
                                        channel.id(),
                                        attemptCount,
                                        exception.finalCause()));
                        continue;
                    } catch (RuntimeException exception) {
                        failedTaskCount++;
                        metricsPort.recordExecution(
                                JOB_NAME, channel.name(), BatchExecutionOutcome.FAILURE);
                        log.error(
                                "batch task failed job={} itemId={} channel={} attempt={} errorType={} durationMs={}",
                                JOB_NAME,
                                item.id(),
                                channel.name(),
                                attemptCount,
                                errorType(exception),
                                elapsedMillis(taskStartedAt));
                        throw exception;
                    } finally {
                        metricsPort.recordRetries(JOB_NAME, channel.name(), attemptCount - 1);
                        metricsPort.recordDuration(
                                JOB_NAME,
                                channel.name(),
                                Duration.ofNanos(System.nanoTime() - taskStartedAt));
                    }
                }
            }
            final BatchJobStatus status = status(
                    totalTaskCount, succeededTaskCount, failedTaskCount, hasSkippedChannel);
            final BatchJobCompletion completion =
                    new BatchJobCompletion(status, totalTaskCount, succeededTaskCount);
            batchJobPersistencePort.finish(jobExecutionId, completion);
            logJobFinished(
                    jobExecutionId,
                    completion.status(),
                    totalTaskCount,
                    succeededTaskCount,
                    failedTaskCount,
                    jobStartedAt);
            return new OnlinePriceCollectionResult(
                    totalTaskCount, succeededTaskCount, failedTaskCount, savedPriceCount);
        } catch (RuntimeException exception) {
            failJob(jobExecutionId, totalTaskCount, succeededTaskCount, exception);
            logJobFinished(
                    jobExecutionId,
                    BatchJobStatus.FAILED,
                    totalTaskCount,
                    succeededTaskCount,
                    failedTaskCount,
                    jobStartedAt);
            throw exception;
        }
    }

    private CrawlExecution crawlWithRetry(
            final Item item,
            final OnlineChannel channel,
            final LocalDate collectionDate) {
        final long startedAt = System.nanoTime();
        for (int attempt = 1; attempt <= MAX_ATTEMPT_COUNT; attempt++) {
            try {
                return new CrawlExecution(crawl(item, channel, collectionDate), attempt);
            } catch (RuntimeException exception) {
                if (attempt == MAX_ATTEMPT_COUNT || !isRetryable(exception)) {
                    throw new CrawlFailure(attempt, safeCause(exception));
                }
                log.warn(
                        "batch task retry job={} itemId={} channel={} attempt={} errorType={} durationMs={}",
                        JOB_NAME,
                        item.id(),
                        channel.name(),
                        attempt,
                        errorType(exception),
                        elapsedMillis(startedAt));
                retryDelayPort.delay(RETRY_DELAYS.get(attempt - 1));
            }
        }
        throw new IllegalStateException("unreachable retry state");
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

    private boolean isRetryable(final RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.httpStatus().value() == 429
                    || (apiException.errorType() == ErrorType.EXTERNAL_API_ERROR
                    && apiException.httpStatus().is5xxServerError());
        }
        return hasNetworkCause(exception);
    }

    private boolean hasNetworkCause(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof IOException || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String errorType(final RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.errorType().name();
        }
        if (hasNetworkCause(exception)) {
            return ErrorType.EXTERNAL_API_ERROR.name();
        }
        return "UNKNOWN_ERROR";
    }

    private RuntimeException safeCause(final RuntimeException exception) {
        if (!hasNetworkCause(exception) || exception instanceof ApiException) {
            return exception;
        }
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY,
                exception);
    }

    private long elapsedMillis(final long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private void logJobFinished(
            final Long jobExecutionId,
            final BatchJobStatus status,
            final int totalTaskCount,
            final int succeededTaskCount,
            final int failedTaskCount,
            final long jobStartedAt) {
        log.info(
                "batch job finished job={} executionId={} status={} total={} succeeded={} failed={} durationMs={}",
                JOB_NAME,
                jobExecutionId,
                status,
                totalTaskCount,
                succeededTaskCount,
                failedTaskCount,
                elapsedMillis(jobStartedAt));
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

    private void failJob(
            final Long jobExecutionId,
            final int totalTaskCount,
            final int succeededTaskCount,
            final RuntimeException cause) {
        try {
            batchJobPersistencePort.finish(
                    jobExecutionId,
                    new BatchJobCompletion(
                            BatchJobStatus.FAILED, totalTaskCount, succeededTaskCount));
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

    private record CrawlExecution(List<OnlinePrice> prices, int attemptCount) {}

    private static final class CrawlFailure extends RuntimeException {

        private final int attemptCount;
        private final RuntimeException finalCause;

        private CrawlFailure(final int attemptCount, final RuntimeException finalCause) {
            this.attemptCount = attemptCount;
            this.finalCause = finalCause;
        }

        private int attemptCount() {
            return attemptCount;
        }

        private RuntimeException finalCause() {
            return finalCause;
        }
    }
}
