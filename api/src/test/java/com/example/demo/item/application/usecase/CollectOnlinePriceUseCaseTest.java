package com.example.demo.item.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.example.demo.item.application.port.OnlinePricePersistencePort;
import com.example.demo.item.application.result.BatchJobStatus;
import com.example.demo.item.application.result.OnlinePriceCollectionResult;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CollectOnlinePriceUseCaseTest {

    private static final LocalDate COLLECTION_DATE = LocalDate.of(2026, 8, 16);

    @Test
    void 마흔여섯개_품목과_네개_채널의_크롤링_결과에서_각_작업별_상위_다섯개만_저장한다() {
        final List<Item> items = itemList(46);
        final List<OnlineChannel> channels = channelList();
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(items, channels, persistence, crawlers(), jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.totalTaskCount()).isEqualTo(184);
        assertThat(result.succeededTaskCount()).isEqualTo(184);
        assertThat(result.failedTaskCount()).isZero();
        assertThat(result.savedPriceCount()).isEqualTo(920);
        assertThat(persistence.savedPrices()).hasSize(184);
        assertThat(persistence.savedPrices().values())
                .allSatisfy(prices -> assertThat(prices).hasSize(5));
        assertThat(persistence.savedPrices().values().stream()
                .flatMap(List::stream)
                .map(OnlinePrice::unit))
                .allMatch(unit -> unit == OnlinePriceCrawlResult.PER_100_GRAMS);
        assertThat(jobs.execution().status()).isEqualTo(BatchJobStatus.COMPLETED);
        assertThat(jobs.execution().totalRecords()).isEqualTo(184);
        assertThat(jobs.execution().successRecords()).isEqualTo(184);
        assertThat(jobs.execution().errorMessage()).isNull();
    }

    @Test
    void 결과가_없는_성공_작업은_기존_가격을_삭제한다() {
        final Item item = item(1L, "감자");
        final OnlineChannel channel = channel(1, "오아시스");
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final Scope scope = new Scope(1L, 1, COLLECTION_DATE);
        persistence.save(scope, List.of(price(item.name(), "기존 상품", 300)));
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item),
                List.of(channel),
                persistence,
                List.of(new StubCrawler("오아시스", name -> List.of())),
                jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(persistence.deletedScopes()).contains(scope);
        assertThat(persistence.savedPrices()).doesNotContainKey(scope);
        assertThat(jobs.execution().status()).isEqualTo(BatchJobStatus.COMPLETED);
    }

    @Test
    void 가격이_없는_결과는_실패하지_않고_저장하지_않는다() {
        final Item item = item(1L, "감자");
        final OnlineChannel channel = channel(1, "오아시스");
        final OnlinePriceCrawlResult result = mock(OnlinePriceCrawlResult.class);
        when(result.price()).thenReturn(null);
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final Scope scope = new Scope(1L, 1, COLLECTION_DATE);
        persistence.save(scope, List.of(price(item.name(), "기존 상품", 300)));
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item),
                List.of(channel),
                persistence,
                List.of(new StubCrawler("오아시스", name -> List.of(result))),
                new InMemoryBatchJobPersistence());

        final OnlinePriceCollectionResult collectionResult = useCase.execute(COLLECTION_DATE);

        assertThat(collectionResult.succeededTaskCount()).isEqualTo(1);
        assertThat(collectionResult.failedTaskCount()).isZero();
        assertThat(collectionResult.savedPriceCount()).isZero();
        assertThat(persistence.deletedScopes()).contains(new Scope(1L, 1, COLLECTION_DATE));
        assertThat(persistence.savedPrices()).isEmpty();
    }

    @Test
    void 같은_URL의_크롤링_결과는_한건만_저장한다() {
        final Item item = item(1L, "감자");
        final OnlineChannel channel = channel(1, "오아시스");
        final URI productUrl = URI.create("https://example.com/same-product");
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item),
                List.of(channel),
                persistence,
                List.of(new StubCrawler("오아시스", name -> List.of(
                        new OnlinePriceCrawlResult(
                                name, "상품 A", BigDecimal.valueOf(200), 100, productUrl, null),
                        new OnlinePriceCrawlResult(
                                name, "상품 B", BigDecimal.valueOf(300), 100, productUrl, null)))),
                new InMemoryBatchJobPersistence());

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.savedPriceCount()).isEqualTo(1);
        assertThat(persistence.savedPrices().values().stream().flatMap(List::stream))
                .extracting(OnlinePrice::productName)
                .containsExactly("상품 A");
    }

    @Test
    void 한_작업이_실패해도_다른_작업은_계속하고_실패한_작업의_기존_가격은_보존한다() {
        final Item failedItem = item(1L, "실패 품목");
        final Item succeededItem = item(2L, "성공 품목");
        final OnlineChannel channel = channel(1, "오아시스");
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final Scope failedScope = new Scope(1L, 1, COLLECTION_DATE);
        final OnlinePrice oldPrice = price("실패 품목", "기존 상품", 300);
        persistence.save(failedScope, List.of(oldPrice));
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(failedItem, succeededItem),
                List.of(channel),
                persistence,
                List.of(new StubCrawler("오아시스", name -> {
                    if (name.equals("실패 품목")) {
                        throw new ApiException(
                                "<html>provider raw response</html>",
                                ErrorType.EXTERNAL_API_ERROR,
                                HttpStatus.BAD_GATEWAY);
                    }
                    return List.of(crawlResult(name, "신규 상품", 200));
                })),
                jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.totalTaskCount()).isEqualTo(2);
        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(result.failedTaskCount()).isEqualTo(1);
        assertThat(persistence.savedPrices().get(failedScope)).containsExactly(oldPrice);
        assertThat(persistence.savedPrices()).containsKey(new Scope(2L, 1, COLLECTION_DATE));
        assertThat(jobs.execution().status()).isEqualTo(BatchJobStatus.PARTIAL);
        assertThat(jobs.itemFailures()).singleElement().satisfies(failure -> {
            assertThat(failure.itemId()).isEqualTo(1L);
            assertThat(failure.channelId()).isEqualTo(1);
            assertThat(failure.attemptCount()).isEqualTo(5);
            assertThat(failure.cause()).isInstanceOf(ApiException.class);
        });
    }

    @Test
    void 재시도_가능한_503_오류는_다섯번째_시도에서_성공하면_가격을_저장한다() {
        final List<Integer> attempts = new ArrayList<>();
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                persistence,
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    if (attempts.size() < 5) {
                        throw new ApiException(
                                "provider unavailable",
                                ErrorType.EXTERNAL_API_ERROR,
                                HttpStatus.SERVICE_UNAVAILABLE);
                    }
                    return List.of(crawlResult(name, "신규 상품", 200));
                })),
                jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(attempts).hasSize(5);
        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(result.failedTaskCount()).isZero();
        assertThat(persistence.savedPrices()).containsKey(new Scope(1L, 1, COLLECTION_DATE));
        assertThat(jobs.itemFailures()).isEmpty();
    }

    @Test
    void 재시도_가능한_503_오류가_다섯번_실패하면_시도횟수_5의_실패를_기록한다() {
        final List<Integer> attempts = new ArrayList<>();
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    throw new ApiException(
                            "provider unavailable",
                            ErrorType.EXTERNAL_API_ERROR,
                            HttpStatus.SERVICE_UNAVAILABLE);
                })),
                jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(attempts).hasSize(5);
        assertThat(result.failedTaskCount()).isEqualTo(1);
        assertThat(jobs.itemFailures()).singleElement().satisfies(failure -> {
            assertThat(failure.attemptCount()).isEqualTo(5);
            assertThat(failure.cause()).isInstanceOf(ApiException.class);
        });
    }

    @Test
    void 네번의_503_실패_뒤_성공하면_1분_2분_4분_8분_순서로_대기한다() {
        final List<Integer> attempts = new ArrayList<>();
        final RecordingRetryDelay retryDelay = new RecordingRetryDelay();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    if (attempts.size() <= 4) {
                        throw unavailable();
                    }
                    return List.of(crawlResult(name, "신규 상품", 200));
                })),
                new InMemoryBatchJobPersistence(),
                retryDelay,
                new RecordingBatchMetrics());

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(attempts).hasSize(5);
        assertThat(retryDelay.delays()).containsExactly(
                Duration.ofMinutes(1),
                Duration.ofMinutes(2),
                Duration.ofMinutes(4),
                Duration.ofMinutes(8));
    }

    @Test
    void http_429_오류는_한번_대기한_뒤_재시도한다() {
        final List<Integer> attempts = new ArrayList<>();
        final RecordingRetryDelay retryDelay = new RecordingRetryDelay();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    if (attempts.size() == 1) {
                        throw new ApiException("rate limited", ErrorType.EXTERNAL_API_ERROR, HttpStatus.TOO_MANY_REQUESTS);
                    }
                    return List.of(crawlResult(name, "신규 상품", 200));
                })),
                new InMemoryBatchJobPersistence(),
                retryDelay,
                new RecordingBatchMetrics());

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(attempts).hasSize(2);
        assertThat(retryDelay.delays()).containsExactly(Duration.ofMinutes(1));
    }

    @Test
    void SocketTimeoutException을_감싼_예외는_재시도한다() {
        final List<Integer> attempts = new ArrayList<>();
        final RecordingRetryDelay retryDelay = new RecordingRetryDelay();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    if (attempts.size() == 1) {
                        throw new RuntimeException(new SocketTimeoutException("timed out"));
                    }
                    return List.of(crawlResult(name, "신규 상품", 200));
                })),
                new InMemoryBatchJobPersistence(),
                retryDelay,
                new RecordingBatchMetrics());

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(attempts).hasSize(2);
        assertThat(retryDelay.delays()).containsExactly(Duration.ofMinutes(1));
    }

    @Test
    void IOException을_감싼_예외는_재시도한다() {
        final List<Integer> attempts = new ArrayList<>();
        final RecordingRetryDelay retryDelay = new RecordingRetryDelay();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    if (attempts.size() == 1) {
                        throw new RuntimeException(new IOException("connection reset"));
                    }
                    return List.of(crawlResult(name, "신규 상품", 200));
                })),
                new InMemoryBatchJobPersistence(),
                retryDelay,
                new RecordingBatchMetrics());

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(attempts).hasSize(2);
        assertThat(retryDelay.delays()).containsExactly(Duration.ofMinutes(1));
    }

    @Test
    void 네트워크_최종_실패는_안전한_외부_API_오류로_정규화한다() {
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name ->
                        throwNetworkFailure())),
                jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.failedTaskCount()).isEqualTo(1);
        assertThat(jobs.itemFailures()).singleElement().satisfies(failure -> {
            assertThat(failure.attemptCount()).isEqualTo(5);
            assertThat(failure.cause()).isInstanceOfSatisfying(ApiException.class, exception -> {
                assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                assertThat(exception.errorMessage()).isEqualTo(ErrorType.EXTERNAL_API_ERROR.description());
            });
        });
    }

    @Test
    void http_400_오류는_재시도하지_않는다() {
        final List<Integer> attempts = new ArrayList<>();
        final RecordingRetryDelay retryDelay = new RecordingRetryDelay();
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    throw new ApiException("invalid request", ErrorType.EXTERNAL_API_ERROR, HttpStatus.BAD_REQUEST);
                })),
                jobs,
                retryDelay,
                new RecordingBatchMetrics());

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.failedTaskCount()).isEqualTo(1);
        assertThat(attempts).hasSize(1);
        assertThat(retryDelay.delays()).isEmpty();
        assertThat(jobs.itemFailures()).singleElement()
                .extracting(BatchItemFailure::attemptCount)
                .isEqualTo(1);
    }

    @Test
    void 빈_성공_결과에는_재시도하지_않고_낮은_카디널리티_메트릭만_기록한다() {
        final List<Integer> attempts = new ArrayList<>();
        final RecordingRetryDelay retryDelay = new RecordingRetryDelay();
        final RecordingBatchMetrics metrics = new RecordingBatchMetrics();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    return List.of();
                })),
                new InMemoryBatchJobPersistence(),
                retryDelay,
                metrics);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(attempts).hasSize(1);
        assertThat(retryDelay.delays()).isEmpty();
        assertThat(metrics.executions()).containsExactly(
                new ExecutionMetric("ONLINE_PRICE_COLLECTION", "오아시스", BatchExecutionOutcome.SUCCESS));
        assertThat(metrics.retries()).containsExactly(new RetryMetric("ONLINE_PRICE_COLLECTION", "오아시스", 0));
        assertThat(metrics.durations()).hasSize(1);
        assertThat(metrics.durations().get(0).job()).isEqualTo("ONLINE_PRICE_COLLECTION");
        assertThat(metrics.durations().get(0).channel()).isEqualTo("오아시스");
    }

    @Test
    void parsing_오류는_최초_실패만_기록하고_재시도하지_않는다() {
        final List<Integer> attempts = new ArrayList<>();
        final RecordingRetryDelay retryDelay = new RecordingRetryDelay();
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    attempts.add(1);
                    throw new ApiException(
                            ErrorType.UNKNOWN_ERROR.description(),
                            ErrorType.UNKNOWN_ERROR,
                            HttpStatus.INTERNAL_SERVER_ERROR);
                })),
                jobs,
                retryDelay,
                new RecordingBatchMetrics());

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.failedTaskCount()).isEqualTo(1);
        assertThat(attempts).hasSize(1);
        assertThat(retryDelay.delays()).isEmpty();
        assertThat(jobs.itemFailures()).singleElement()
                .extracting(BatchItemFailure::attemptCount)
                .isEqualTo(1);
    }

    @Test
    void retry_대기_실패는_현재_작업과_남은_작업을_중단하고_job을_FAILED로_종료한다() {
        final List<String> crawledItems = new ArrayList<>();
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "첫 품목"), item(2L, "둘째 품목")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    crawledItems.add(name);
                    throw unavailable();
                })),
                jobs,
                duration -> {
                    throw new IllegalStateException("retry delay interrupted");
                },
                new RecordingBatchMetrics());

        assertThatThrownBy(() -> useCase.execute(COLLECTION_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("retry delay interrupted");
        assertThat(crawledItems).containsExactly("첫 품목");
        assertThat(jobs.execution().status()).isEqualTo(BatchJobStatus.FAILED);
        assertThat(jobs.itemFailures()).isEmpty();
    }

    @Test
    void 등록된_crawler가_없는_채널은_작업수와_실패수에서_제외한다() {
        final Item item = item(1L, "감자");
        final OnlineChannel runnableChannel = channel(1, "오아시스");
        final OnlineChannel missingChannel = channel(2, "미등록 채널");
        final InMemoryOnlinePricePersistence persistence = new InMemoryOnlinePricePersistence();
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item),
                List.of(runnableChannel, missingChannel),
                persistence,
                List.of(new StubCrawler("오아시스", name -> List.of(crawlResult(name, "신규 상품", 200)))),
                jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.totalTaskCount()).isEqualTo(1);
        assertThat(result.succeededTaskCount()).isEqualTo(1);
        assertThat(result.failedTaskCount()).isZero();
        assertThat(jobs.execution().status()).isEqualTo(BatchJobStatus.PARTIAL);
        assertThat(jobs.itemFailures()).isEmpty();
    }

    @Test
    void 실행할_crawler가_하나도_없으면_job은_실패하고_item_error는_남기지_않는다() {
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "미등록 채널")),
                new InMemoryOnlinePricePersistence(),
                List.of(),
                jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.totalTaskCount()).isZero();
        assertThat(jobs.execution().status()).isEqualTo(BatchJobStatus.FAILED);
        assertThat(jobs.execution().errorMessage()).isNotBlank();
        assertThat(jobs.itemFailures()).isEmpty();
    }

    @Test
    void 모든_실행_작업이_실패하면_job은_FAILED다() {
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    throw new IllegalStateException("raw provider body");
                })),
                jobs);

        final OnlinePriceCollectionResult result = useCase.execute(COLLECTION_DATE);

        assertThat(result.failedTaskCount()).isEqualTo(1);
        assertThat(jobs.execution().status()).isEqualTo(BatchJobStatus.FAILED);
        assertThat(jobs.execution().errorMessage()).isNotBlank();
        assertThat(jobs.itemFailures()).singleElement().satisfies(failure -> {
            assertThat(failure.itemId()).isEqualTo(1L);
            assertThat(failure.channelId()).isEqualTo(1);
            assertThat(failure.attemptCount()).isEqualTo(1);
            assertThat(failure.cause()).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void job_초기화가_실패하면_crawler를_실행하지_않는다() {
        final List<String> calledItems = new ArrayList<>();
        final InMemoryBatchJobPersistence jobs = new InMemoryBatchJobPersistence();
        jobs.failOnStart();
        final CollectOnlinePriceUseCase useCase = useCase(
                List.of(item(1L, "감자")),
                List.of(channel(1, "오아시스")),
                new InMemoryOnlinePricePersistence(),
                List.of(new StubCrawler("오아시스", name -> {
                    calledItems.add(name);
                    return List.of();
                })),
                jobs);

        assertThatThrownBy(() -> useCase.execute(COLLECTION_DATE))
                .isInstanceOf(IllegalStateException.class);
        assertThat(calledItems).isEmpty();
    }

    private CollectOnlinePriceUseCase useCase(
            final List<Item> items,
            final List<OnlineChannel> channels,
            final InMemoryOnlinePricePersistence persistence,
            final List<OnlinePriceCrawlerPort> crawlers,
            final BatchJobPersistencePort batchJobPersistencePort) {
        final OnlineItemQueryPort itemQueryPort = () -> items;
        final OnlineChannelQueryPort channelQueryPort = () -> channels;
        final ReplaceOnlinePriceUseCase replaceUseCase = new ReplaceOnlinePriceUseCase(persistence);
        return new CollectOnlinePriceUseCase(
                itemQueryPort,
                channelQueryPort,
                crawlers,
                replaceUseCase,
                batchJobPersistencePort,
                duration -> {},
                new RecordingBatchMetrics());
    }

    private CollectOnlinePriceUseCase useCase(
            final List<Item> items,
            final List<OnlineChannel> channels,
            final InMemoryOnlinePricePersistence persistence,
            final List<OnlinePriceCrawlerPort> crawlers,
            final BatchJobPersistencePort batchJobPersistencePort,
            final BatchRetryDelayPort retryDelayPort,
            final BatchMetricsPort metricsPort) {
        final OnlineItemQueryPort itemQueryPort = () -> items;
        final OnlineChannelQueryPort channelQueryPort = () -> channels;
        final ReplaceOnlinePriceUseCase replaceUseCase = new ReplaceOnlinePriceUseCase(persistence);
        return new CollectOnlinePriceUseCase(
                itemQueryPort,
                channelQueryPort,
                crawlers,
                replaceUseCase,
                batchJobPersistencePort,
                retryDelayPort,
                metricsPort);
    }

    private ApiException unavailable() {
        return new ApiException("provider unavailable", ErrorType.EXTERNAL_API_ERROR, HttpStatus.SERVICE_UNAVAILABLE);
    }

    private List<OnlinePriceCrawlResult> throwNetworkFailure() {
        throw new RuntimeException(new IOException("raw provider response"));
    }

    private List<OnlinePriceCrawlerPort> crawlers() {
        return channelList().stream()
                .map(channel -> new StubCrawler(channel.name(), name -> List.of(
                        crawlResult(name, "상품 1", 101),
                        crawlResult(name, "상품 2", 102),
                        crawlResult(name, "상품 3", 103),
                        crawlResult(name, "상품 4", 104),
                        crawlResult(name, "상품 5", 105),
                        crawlResult(name, "상품 6", 106))))
                .map(crawler -> (OnlinePriceCrawlerPort) crawler)
                .toList();
    }

    private List<Item> itemList(final int count) {
        final List<Item> items = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            items.add(item(id, "품목" + id));
        }
        return items;
    }

    private List<OnlineChannel> channelList() {
        return List.of(
                channel(1, "오아시스"),
                channel(2, "컬리"),
                channel(3, "11번가"),
                channel(4, "GS SHOP"));
    }

    private Item item(final Long id, final String name) {
        final Item item = mock(Item.class);
        when(item.id()).thenReturn(id);
        when(item.name()).thenReturn(name);
        return item;
    }

    private OnlineChannel channel(final Integer id, final String name) {
        final OnlineChannel channel = mock(OnlineChannel.class);
        when(channel.id()).thenReturn(id);
        when(channel.name()).thenReturn(name);
        return channel;
    }

    private OnlinePrice price(final String itemName, final String productName, final int value) {
        return new OnlinePrice(
                1L, 1, itemName, productName, value, OnlinePriceCrawlResult.PER_100_GRAMS,
                "https://example.com/old", null, COLLECTION_DATE);
    }

    private static OnlinePriceCrawlResult crawlResult(
            final String itemName, final String productName, final int value) {
        return new OnlinePriceCrawlResult(
                itemName,
                productName,
                BigDecimal.valueOf(value),
                OnlinePriceCrawlResult.PER_100_GRAMS,
                URI.create("https://example.com/" + productName.replace(' ', '-')),
                null);
    }

    private record Scope(Long itemId, Integer channelId, LocalDate collectionDate) {}

    private record JobExecution(
            BatchJobStatus status,
            int totalRecords,
            int successRecords,
            String errorMessage) {}

    private static final class StubCrawler implements OnlinePriceCrawlerPort {

        private final String channelName;
        private final Function<String, List<OnlinePriceCrawlResult>> results;

        private StubCrawler(
                final String channelName,
                final Function<String, List<OnlinePriceCrawlResult>> results) {
            this.channelName = channelName;
            this.results = results;
        }

        @Override
        public String channelName() {
            return channelName;
        }

        @Override
        public List<OnlinePriceCrawlResult> crawl(final CrawlOnlinePriceCommand command) {
            return results.apply(command.itemName());
        }
    }

    private static final class InMemoryOnlinePricePersistence implements OnlinePricePersistencePort {

        private final Map<Scope, List<OnlinePrice>> savedPrices = new HashMap<>();
        private final List<Scope> deletedScopes = new ArrayList<>();

        @Override
        public void deleteAll(final Long itemId, final Integer channelId, final LocalDate collectionDate) {
            final Scope scope = new Scope(itemId, channelId, collectionDate);
            deletedScopes.add(scope);
            savedPrices.remove(scope);
        }

        @Override
        public void saveAll(final List<OnlinePrice> prices) {
            if (prices.isEmpty()) {
                return;
            }
            final OnlinePrice first = prices.get(0);
            savedPrices.put(
                    new Scope(first.itemId(), first.channelId(), first.createdAt()),
                    List.copyOf(prices));
        }

        private void save(final Scope scope, final List<OnlinePrice> prices) {
            savedPrices.put(scope, List.copyOf(prices));
        }

        private Map<Scope, List<OnlinePrice>> savedPrices() {
            return savedPrices;
        }

        private List<Scope> deletedScopes() {
            return deletedScopes;
        }
    }

    private static final class InMemoryBatchJobPersistence implements BatchJobPersistencePort {

        private static final Long JOB_EXECUTION_ID = 1L;

        private JobExecution execution;
        private final List<BatchItemFailure> itemFailures = new ArrayList<>();
        private boolean failOnStart;

        @Override
        public Long start(final String jobName) {
            if (failOnStart) {
                throw new IllegalStateException("job initialization failed");
            }
            execution = new JobExecution(BatchJobStatus.STARTED, 0, 0, null);
            return JOB_EXECUTION_ID;
        }

        @Override
        public void recordItemError(
                final Long jobExecutionId,
                final BatchItemFailure failure) {
            itemFailures.add(failure);
        }

        @Override
        public void finish(
                final Long jobExecutionId,
                final BatchJobCompletion completion) {
            execution = new JobExecution(
                    completion.status(),
                    completion.totalRecords(),
                    completion.successRecords(),
                    completion.errorMessage());
        }

        private JobExecution execution() {
            return execution;
        }

        private List<BatchItemFailure> itemFailures() {
            return itemFailures;
        }

        private void failOnStart() {
            failOnStart = true;
        }
    }

    private static final class RecordingRetryDelay implements BatchRetryDelayPort {

        private final List<Duration> delays = new ArrayList<>();

        @Override
        public void delay(final Duration duration) {
            delays.add(duration);
        }

        private List<Duration> delays() {
            return delays;
        }
    }

    private static final class RecordingBatchMetrics implements BatchMetricsPort {

        private final List<ExecutionMetric> executions = new ArrayList<>();
        private final List<RetryMetric> retries = new ArrayList<>();
        private final List<DurationMetric> durations = new ArrayList<>();

        @Override
        public void recordExecution(
                final String job,
                final String channel,
                final BatchExecutionOutcome outcome) {
            executions.add(new ExecutionMetric(job, channel, outcome));
        }

        @Override
        public void recordRetries(final String job, final String channel, final int retryCount) {
            retries.add(new RetryMetric(job, channel, retryCount));
        }

        @Override
        public void recordDuration(final String job, final String channel, final Duration duration) {
            durations.add(new DurationMetric(job, channel, duration));
        }

        private List<ExecutionMetric> executions() {
            return executions;
        }

        private List<RetryMetric> retries() {
            return retries;
        }

        private List<DurationMetric> durations() {
            return durations;
        }
    }

    private record ExecutionMetric(String job, String channel, BatchExecutionOutcome outcome) {}

    private record RetryMetric(String job, String channel, int retryCount) {}

    private record DurationMetric(String job, String channel, Duration duration) {}
}
