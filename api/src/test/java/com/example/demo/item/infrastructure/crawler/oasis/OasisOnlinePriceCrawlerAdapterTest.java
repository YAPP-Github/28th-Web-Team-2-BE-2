package com.example.demo.item.infrastructure.crawler.oasis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class OasisOnlinePriceCrawlerAdapterTest {

    private final OasisOnlineItemCrawler crawler = mock(OasisOnlineItemCrawler.class);
    private final OasisOnlinePriceCrawlerAdapter adapter = new OasisOnlinePriceCrawlerAdapter(crawler);

    @Test
    void 오아시스_채널명을_반환한다() {
        assertThat(adapter.channelName()).isEqualTo("오아시스");
    }

    @Test
    void convertsOasisProductToCommonPriceResult() {
        final OasisProduct product = new OasisProduct(
                "59370",
                "GAP 말랑촉촉 청도 감자",
                URI.create("https://www.oasis.co.kr/product/detail/59370"),
                BigDecimal.valueOf(2900),
                BigDecimal.valueOf(3300),
                BigDecimal.valueOf(2900),
                "오아시스배송");
        when(crawler.crawl("감자")).thenReturn(List.of(product));

        final List<OnlinePriceCrawlResult> results = adapter.crawl(new CrawlOnlinePriceCommand("감자"));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.itemName()).isEqualTo("감자");
            assertThat(result.productName()).isEqualTo("GAP 말랑촉촉 청도 감자");
            assertThat(result.price()).isEqualByComparingTo("2900");
            assertThat(result.unit()).isEqualTo(OnlinePriceCrawlResult.PER_100_GRAMS);
            assertThat(result.productUrl()).isEqualTo(product.productUrl());
            assertThat(result.deliveryNote()).isEqualTo("오아시스배송");
        });
    }
}
