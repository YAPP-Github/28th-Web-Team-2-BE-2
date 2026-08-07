package com.example.demo.price.application.usecase;

import com.example.demo.price.application.port.CollectionExecutionRepository;
import com.example.demo.price.application.port.OnlinePriceRepository;
import com.example.demo.price.application.command.CollectionTask;
import com.example.demo.price.application.result.CollectionResult;
import com.example.demo.price.application.result.CrawlResult;
import com.example.demo.price.domain.CollectionStatus;
import com.example.demo.price.domain.NormalizedPrice;
import com.example.demo.price.domain.RawOffer;
import com.example.demo.price.domain.matching.ProductMatcher;
import com.example.demo.price.domain.normalization.PriceNormalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectPriceTaskUseCase {

    private static final int MINIMUM_SAMPLE_COUNT = 3;

    private final CrawlPriceTaskUseCase crawlPriceTaskUseCase;
    private final ProductMatcher productMatcher;
    private final PriceNormalizer priceNormalizer;
    private final OnlinePriceRepository onlinePriceRepository;
    private final CollectionExecutionRepository collectionExecutionRepository;

    public CollectionResult execute(final CollectionTask task) {
        final OffsetDateTime startedAt = OffsetDateTime.now();
        try {
            final CrawlResult crawlResult = crawlPriceTaskUseCase.execute(task);
            if (crawlResult.accessStatus() == CrawlResult.AccessStatus.BLOCKED) {
                return record(task, CollectionStatus.BLOCKED, startedAt, 0, crawlResult.failureReason());
            }
            final int validOfferCount = persistValidOffers(task, crawlResult.offers());
            final CollectionStatus status = validOfferCount < MINIMUM_SAMPLE_COUNT
                    ? CollectionStatus.INSUFFICIENT_SAMPLE
                    : CollectionStatus.SUCCEEDED;
            return record(task, status, startedAt, validOfferCount, crawlResult.failureReason());
        } catch (RuntimeException exception) {
            return record(task, CollectionStatus.FAILED, startedAt, 0, exception.getMessage());
        }
    }

    private int persistValidOffers(final CollectionTask task, final List<RawOffer> offers) {
        int validOfferCount = 0;
        for (RawOffer offer : offers) {
            if (!productMatcher.matches(task.itemName(), offer)) {
                continue;
            }
            final Optional<NormalizedPrice> normalized = priceNormalizer.normalize(offer, task.targetUnit());
            if (normalized.isEmpty()) {
                continue;
            }
            onlinePriceRepository.upsert(new OnlinePriceRepository.DailyProductPrice(
                    task.itemId(), task.channel(), task.itemName(), offer.title(), offer.productUrl(),
                    normalized.orElseThrow(), task.priceDate()));
            validOfferCount++;
        }
        return validOfferCount;
    }

    private CollectionResult record(
            final CollectionTask task,
            final CollectionStatus status,
            final OffsetDateTime startedAt,
            final int validOfferCount,
            final String failureReason) {
        collectionExecutionRepository.record(new CollectionExecutionRepository.TaskExecution(
                task.executionId(), task.itemId(), task.itemName(), task.channel().name(), status,
                startedAt, OffsetDateTime.now(), validOfferCount, failureReason));
        return new CollectionResult(status, validOfferCount, failureReason);
    }
}
