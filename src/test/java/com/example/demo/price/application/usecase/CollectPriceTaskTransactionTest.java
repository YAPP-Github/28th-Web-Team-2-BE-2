package com.example.demo.price.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.price.application.command.CollectionTask;
import com.example.demo.price.application.port.ChannelCrawler;
import com.example.demo.price.application.result.CrawlResult;
import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.PriceUnit;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(CollectPriceTaskTransactionTest.TestBeans.class)
class CollectPriceTaskTransactionTest {

    @Autowired
    private CollectPriceTaskUseCase collectPriceTaskUseCase;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FakeCrawler fakeCrawler;

    @Test
    void 크롤링은_저장_트랜잭션_없이_실행된다() {
        transactionTemplate.execute(status -> {
            collectPriceTaskUseCase.execute(task());
            return null;
        });

        assertThat(fakeCrawler.transactionActive()).isFalse();
    }

    private CollectionTask task() {
        return new CollectionTask(1L, "감자", ChannelCode.KURLY, PriceUnit.KG,
                LocalDate.of(2026, 8, 7), 10L);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        FakeCrawler fakeCrawler() {
            return new FakeCrawler();
        }

        @Bean
        ChannelCrawler kurlyCrawler(final FakeCrawler fakeCrawler) {
            return fakeCrawler;
        }
    }

    static class FakeCrawler implements ChannelCrawler {

        private final AtomicBoolean transactionActive = new AtomicBoolean();

        @Override
        public CrawlResult crawl(final CollectionTask task) {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return new CrawlResult(List.of(), "https://example.com", OffsetDateTime.now(),
                    CrawlResult.AccessStatus.SUCCESS, null);
        }

        @Override
        public ChannelCode channel() {
            return ChannelCode.KURLY;
        }

        boolean transactionActive() {
            return transactionActive.get();
        }
    }
}
