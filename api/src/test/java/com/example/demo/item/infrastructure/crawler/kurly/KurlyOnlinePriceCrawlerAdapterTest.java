package com.example.demo.item.infrastructure.crawler.kurly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class KurlyOnlinePriceCrawlerAdapterTest {

    private final KurlyOnlineItemCrawler crawler = mock(KurlyOnlineItemCrawler.class);
    private final KurlyOnlinePriceCrawlerAdapter adapter = new KurlyOnlinePriceCrawlerAdapter(crawler);

    @Test
    void 컬리_채널명을_반환한다() {
        assertThat(adapter.channelName()).isEqualTo("컬리");
    }

    @Test
    void convertsKurlyProductsToApplicationPriceResults() {
        final KurlyProduct product = new KurlyProduct(
                "5026448",
                "[KF365] 감자 1kg",
                URI.create("https://www.kurly.com/goods/5026448"),
                BigDecimal.valueOf(3990),
                BigDecimal.valueOf(4990))
                .withPricePer100g(BigDecimal.valueOf(399))
                .withDeliveryNote("샛별배송");
        when(crawler.crawl("감자")).thenReturn(List.of(product));

        final List<OnlinePriceCrawlResult> results = adapter.crawl(new CrawlOnlinePriceCommand("감자"));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.itemName()).isEqualTo("감자");
            assertThat(result.productName()).isEqualTo("[KF365] 감자 1kg");
            assertThat(result.price()).isEqualByComparingTo("399");
            assertThat(result.unit()).isEqualTo(OnlinePriceCrawlResult.PER_100_GRAMS);
            assertThat(result.productUrl()).isEqualTo(product.productUrl());
            assertThat(result.deliveryNote()).isEqualTo("샛별배송");
        });
    }
}
