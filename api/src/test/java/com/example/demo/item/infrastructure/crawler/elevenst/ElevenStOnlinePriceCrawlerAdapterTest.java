package com.example.demo.item.infrastructure.crawler.elevenst;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElevenStOnlinePriceCrawlerAdapterTest {

    @Test
    void 십일번가_채널명을_반환한다() {
        final ElevenStOnlinePriceCrawlerAdapter adapter =
                new ElevenStOnlinePriceCrawlerAdapter(mock(ElevenStOnlineItemCrawler.class));

        assertThat(adapter.channelName()).isEqualTo("11번가");
    }

    @Test
    void convertsElevenStProductToCommonResult() {
        final ElevenStOnlineItemCrawler crawler = mock(ElevenStOnlineItemCrawler.class);
        final ElevenStProduct product = new ElevenStProduct(
                "879232236", "햇감자 5kg", URI.create("https://www.11st.co.kr/products/879232236"),
                BigDecimal.valueOf(8900), BigDecimal.valueOf(9900), BigDecimal.valueOf(890), "무료배송");
        when(crawler.crawl("감자")).thenReturn(List.of(product));

        final List<OnlinePriceCrawlResult> results = new ElevenStOnlinePriceCrawlerAdapter(crawler)
                .crawl(new CrawlOnlinePriceCommand("감자"));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.productName()).isEqualTo("햇감자 5kg");
            assertThat(result.price()).isEqualByComparingTo("890");
            assertThat(result.unit()).isEqualTo(OnlinePriceCrawlResult.PER_100_GRAMS);
            assertThat(result.deliveryNote()).isEqualTo("무료배송");
        });
    }
}
