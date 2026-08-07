package com.example.demo.price.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.application.port.ChannelCrawler;
import com.example.demo.price.application.port.CollectionExecutionRepository;
import com.example.demo.price.application.port.OnlinePriceRepository;
import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.CollectionStatus;
import com.example.demo.price.application.command.CollectionTask;
import com.example.demo.price.application.result.CrawlResult;
import com.example.demo.price.domain.ParsedQuantity;
import com.example.demo.price.domain.PriceUnit;
import com.example.demo.price.domain.RawOffer;
import com.example.demo.price.domain.matching.ProductMatcher;
import com.example.demo.price.domain.normalization.PriceNormalizer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CollectPriceTaskUseCaseTest {

    @Test
    void 유효한_후보를_저장하고_성공을_기록한다() {
        final RecordingPriceRepository prices = new RecordingPriceRepository();
        final RecordingExecutionRepository executions = new RecordingExecutionRepository();
        final CollectPriceTaskUseCase useCase = new CollectPriceTaskUseCase(
                new CrawlPriceTaskUseCase(List.of(
                        new FakeCrawler(CrawlResult.AccessStatus.SUCCESS, List.of(
                                offer("감자 1kg", "p-1"),
                                offer("감자 2kg", "p-2"),
                                offer("감자 500g", "p-3"))))),
                new ProductMatcher(), new PriceNormalizer(), prices, executions);

        final var result = useCase.execute(task());

        assertThat(result.status()).isEqualTo(CollectionStatus.SUCCEEDED);
        assertThat(prices.saved).hasSize(3);
        assertThat(executions.last.status()).isEqualTo(CollectionStatus.SUCCEEDED);
    }

    @Test
    void 차단된_채널은_가격을_저장하지_않고_blocked로_기록한다() {
        final RecordingPriceRepository prices = new RecordingPriceRepository();
        final RecordingExecutionRepository executions = new RecordingExecutionRepository();
        final CollectPriceTaskUseCase useCase = new CollectPriceTaskUseCase(
                new CrawlPriceTaskUseCase(List.of(new FakeCrawler(CrawlResult.AccessStatus.BLOCKED, List.of()))),
                new ProductMatcher(), new PriceNormalizer(), prices, executions);

        final var result = useCase.execute(task());

        assertThat(result.status()).isEqualTo(CollectionStatus.BLOCKED);
        assertThat(prices.saved).isEmpty();
        assertThat(executions.last.status()).isEqualTo(CollectionStatus.BLOCKED);
    }

    @Test
    void 후보가_부족하면_가격은_저장해도_insufficient_sample로_기록한다() {
        final RecordingPriceRepository prices = new RecordingPriceRepository();
        final RecordingExecutionRepository executions = new RecordingExecutionRepository();
        final CollectPriceTaskUseCase useCase = new CollectPriceTaskUseCase(
                new CrawlPriceTaskUseCase(List.of(new FakeCrawler(CrawlResult.AccessStatus.SUCCESS,
                        List.of(offer("감자 1kg", "p-1"))))),
                new ProductMatcher(), new PriceNormalizer(), prices, executions);

        final var result = useCase.execute(task());

        assertThat(result.status()).isEqualTo(CollectionStatus.INSUFFICIENT_SAMPLE);
        assertThat(executions.last.status()).isEqualTo(CollectionStatus.INSUFFICIENT_SAMPLE);
    }

    @Test
    void 등록되지_않은_채널은_failed로_기록한다() {
        final RecordingExecutionRepository executions = new RecordingExecutionRepository();
        final CollectPriceTaskUseCase useCase = new CollectPriceTaskUseCase(
                new CrawlPriceTaskUseCase(List.of()), new ProductMatcher(), new PriceNormalizer(),
                new RecordingPriceRepository(), executions);

        final var result = useCase.execute(task());

        assertThat(result.status()).isEqualTo(CollectionStatus.FAILED);
        assertThat(executions.last.status()).isEqualTo(CollectionStatus.FAILED);
    }

    private CollectionTask task() {
        return new CollectionTask(1L, "감자", ChannelCode.OASIS, PriceUnit.KG,
                LocalDate.of(2026, 8, 7), 10L);
    }

    private RawOffer offer(final String title, final String externalId) {
        return new RawOffer(externalId, title, "https://example.com/" + externalId,
                BigDecimal.valueOf(1000), BigDecimal.ZERO,
                new ParsedQuantity(BigDecimal.ONE, PriceUnit.KG), "국내산", true, false);
    }

    private static class FakeCrawler implements ChannelCrawler {

        private final CrawlResult result;

        private FakeCrawler(final CrawlResult.AccessStatus status, final List<RawOffer> offers) {
            result = new CrawlResult(offers, "https://example.com", OffsetDateTime.now(), status, "blocked");
        }

        @Override
        public CrawlResult crawl(final CollectionTask task) {
            return result;
        }

        @Override
        public ChannelCode channel() {
            return ChannelCode.OASIS;
        }
    }

    private static class RecordingPriceRepository implements OnlinePriceRepository {

        private final List<DailyProductPrice> saved = new ArrayList<>();

        @Override
        public void upsert(final DailyProductPrice price) {
            saved.add(price);
        }
    }

    private static class RecordingExecutionRepository implements CollectionExecutionRepository {

        private TaskExecution last;

        @Override
        public void record(final TaskExecution execution) {
            last = execution;
        }
    }
}
