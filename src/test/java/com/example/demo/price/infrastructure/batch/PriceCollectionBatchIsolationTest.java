package com.example.demo.price.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.application.command.CollectionTask;
import com.example.demo.price.application.port.ChannelCrawler;
import com.example.demo.price.application.port.CollectionTaskProvider;
import com.example.demo.price.application.result.CrawlResult;
import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.ParsedQuantity;
import com.example.demo.price.domain.PriceUnit;
import com.example.demo.price.domain.RawOffer;
import com.example.demo.price.infrastructure.CollectionExecutionJpaRepository;
import com.example.demo.price.infrastructure.OnlinePriceJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@SpringBootTest
@Import(PriceCollectionBatchIsolationTest.TestBeans.class)
class PriceCollectionBatchIsolationTest {

    private static final LocalDate COLLECTION_DATE = LocalDate.of(2026, 8, 7);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job priceCollectionJob;

    @Autowired
    private CollectionTaskProvider collectionTaskProvider;

    @Autowired
    private FakeCrawler fakeCrawler;

    @Autowired
    private CollectionExecutionJpaRepository collectionExecutionJpaRepository;

    @Autowired
    private OnlinePriceJpaRepository onlinePriceJpaRepository;

    @BeforeEach
    void setUp() {
        collectionExecutionJpaRepository.deleteAll();
        onlinePriceJpaRepository.deleteAll();
        fakeCrawler.reset();
    }

    @Test
    void 한_task가_실패해도_다음_task를_계속_처리한다() throws Exception {
        final JobParameters parameters = new JobParametersBuilder()
                .addString("priceDate", COLLECTION_DATE.toString())
                .addString("executionKey", UUID.randomUUID().toString())
                .toJobParameters();

        assertThat(collectionTaskProvider.activeTasks(COLLECTION_DATE, 10L)).hasSize(2);

        final JobExecution jobExecution = jobLauncher.run(priceCollectionJob, parameters);

        assertThat(jobExecution.getStatus())
                .as("job failure exceptions: %s", jobExecution.getAllFailureExceptions())
                .isEqualTo(BatchStatus.COMPLETED);
        assertThat(fakeCrawler.callCount()).isEqualTo(2);
        assertThat(collectionExecutionJpaRepository.count()).isEqualTo(2);
        assertThat(onlinePriceJpaRepository.count()).isEqualTo(3);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        FakeCrawler fakeCrawler() {
            return new FakeCrawler();
        }

        @Bean
        @Primary
        CollectionTaskProvider collectionTaskProvider() {
            return (priceDate, executionId) -> List.of(
                    task(priceDate, executionId, 1L),
                    task(priceDate, executionId, 2L));
        }

        private CollectionTask task(final LocalDate priceDate, final Long executionId, final Long itemId) {
            return new CollectionTask(itemId, "감자", ChannelCode.KURLY, PriceUnit.KG,
                    priceDate, executionId);
        }
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    static class FakeCrawler implements ChannelCrawler {

        private final AtomicInteger callCount = new AtomicInteger();

        @Override
        public CrawlResult crawl(final CollectionTask task) {
            if (callCount.getAndIncrement() == 0) {
                throw new IllegalStateException("temporary crawler failure");
            }
            return new CrawlResult(List.of(
                    offer("감자 1kg", "p-1"),
                    offer("감자 2kg", "p-2"),
                    offer("감자 500g", "p-3")),
                    "https://example.com", OffsetDateTime.now(),
                    CrawlResult.AccessStatus.SUCCESS, null);
        }

        @Override
        public ChannelCode channel() {
            return ChannelCode.KURLY;
        }

        int callCount() {
            return callCount.get();
        }

        void reset() {
            callCount.set(0);
        }

        private RawOffer offer(final String title, final String externalId) {
            return new RawOffer(externalId, title, "https://example.com/" + externalId,
                    BigDecimal.valueOf(1000), BigDecimal.ZERO,
                    new ParsedQuantity(BigDecimal.ONE, PriceUnit.KG), "국내산", true, false);
        }
    }
}
