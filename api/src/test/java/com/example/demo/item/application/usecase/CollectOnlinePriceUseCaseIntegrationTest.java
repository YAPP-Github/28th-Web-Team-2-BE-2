package com.example.demo.item.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.port.OnlinePriceCrawlerPort;
import com.example.demo.item.application.result.BatchJobStatus;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.OnlineChannel;
import com.example.demo.item.domain.OnlinePrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.OnlineChannelJpaRepository;
import com.example.demo.item.infrastructure.OnlinePriceJpaRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(CollectOnlinePriceUseCaseIntegrationTest.TestConfig.class)
class CollectOnlinePriceUseCaseIntegrationTest {

    private static final LocalDate COLLECTION_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private CollectOnlinePriceUseCase useCase;

    @Autowired
    private ItemJpaRepository itemJpaRepository;

    @Autowired
    private OnlineChannelJpaRepository onlineChannelJpaRepository;

    @Autowired
    private OnlinePriceJpaRepository onlinePriceJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTestCrawler crawler;

    private Long firstItemId;
    private Long failedItemId;
    private Long unprocessedItemId;
    private Integer channelId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM batch_item_errors");
        jdbcTemplate.update("DELETE FROM batch_job_execution");
        onlinePriceJpaRepository.deleteAll();
        onlineChannelJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        firstItemId = itemJpaRepository.save(new Item("첫 성공", "1kg")).id();
        failedItemId = itemJpaRepository.save(new Item("DB 실패", "1kg")).id();
        unprocessedItemId = itemJpaRepository.save(new Item("미실행", "1kg")).id();
        channelId = onlineChannelJpaRepository.save(new OnlineChannel("트랜잭션 테스트")).id();
        onlinePriceJpaRepository.save(oldPrice());
        crawler.reset();
    }

    @Test
    void DB_실패가_발생하면_앞선_커밋과_실패한_작업의_기존값을_보존하고_나머지를_중단한다() {
        assertThatThrownBy(() -> useCase.execute(COLLECTION_DATE))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(prices(firstItemId)).extracting(OnlinePrice::productName)
                .containsExactly("첫 성공 상품");
        assertThat(prices(failedItemId)).extracting(OnlinePrice::productName)
                .containsExactly("기존 상품");
        assertThat(prices(unprocessedItemId)).isEmpty();
        assertThat(crawler.calledItemNames()).containsExactly("첫 성공", "DB 실패");

        final Map<String, Object> execution = jdbcTemplate.queryForMap(
                "SELECT * FROM batch_job_execution ORDER BY job_execution_id DESC LIMIT 1");
        assertThat(execution.get("STATUS")).isEqualTo(BatchJobStatus.FAILED.name());
        assertThat(execution.get("TOTAL_RECORDS")).isEqualTo(3);
        assertThat(execution.get("SUCCESS_RECORDS")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batch_item_errors", Integer.class)).isZero();
    }

    private List<OnlinePrice> prices(final Long itemId) {
        return onlinePriceJpaRepository.findAllByItemIdAndChannelIdAndCreatedAtOrderByIdAsc(
                itemId, channelId, COLLECTION_DATE);
    }

    private OnlinePrice oldPrice() {
        return new OnlinePrice(
                failedItemId,
                channelId,
                "DB 실패",
                "기존 상품",
                300,
                100,
                "https://example.com/old",
                null,
                COLLECTION_DATE);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        TransactionTestCrawler transactionTestCrawler() {
            return new TransactionTestCrawler();
        }
    }

    static final class TransactionTestCrawler implements OnlinePriceCrawlerPort {

        private final List<String> calledItemNames = new ArrayList<>();

        @Override
        public String channelName() {
            return "트랜잭션 테스트";
        }

        @Override
        public List<OnlinePriceCrawlResult> crawl(final CrawlOnlinePriceCommand command) {
            calledItemNames.add(command.itemName());
            if (command.itemName().equals("DB 실패")) {
                return List.of(result(
                        command.itemName(), "가".repeat(256), "https://example.com/too-long-product-name"));
            }
            return List.of(result(
                    command.itemName(), command.itemName() + " 상품", "https://example.com/success"));
        }

        private OnlinePriceCrawlResult result(
                final String itemName,
                final String productName,
                final String productUrl) {
            return new OnlinePriceCrawlResult(
                    itemName,
                    productName,
                    BigDecimal.valueOf(200),
                    OnlinePriceCrawlResult.PER_100_GRAMS,
                    URI.create(productUrl),
                    null);
        }

        private List<String> calledItemNames() {
            return calledItemNames;
        }

        private void reset() {
            calledItemNames.clear();
        }
    }
}
