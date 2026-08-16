package com.example.demo.item.infrastructure.crawler.gsshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.item.application.command.CrawlOnlinePriceCommand;
import com.example.demo.item.application.result.OnlinePriceCrawlResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class GsShopOnlinePriceCrawlerAdapterTest {

    private final GsShopOnlineItemCrawler crawler = mock(GsShopOnlineItemCrawler.class);
    private final GsShopOnlinePriceCrawlerAdapter adapter = new GsShopOnlinePriceCrawlerAdapter(crawler);

    @Test
    void 지에스샵_채널명을_반환한다() {
        assertThat(adapter.channelName()).isEqualTo("GS SHOP");
    }

    @Test
    void convertsGsShopProductToCommonPriceResult() {
        final GsShopProduct product = new GsShopProduct(
                "57668979",
                "감자 5kg",
                URI.create("https://www.gsshop.com/prd/prd.gs?prdid=57668979"),
                BigDecimal.valueOf(8900),
                BigDecimal.valueOf(178),
                "무료배송");
        when(crawler.crawl("감자")).thenReturn(List.of(product));

        final List<OnlinePriceCrawlResult> results = adapter.crawl(new CrawlOnlinePriceCommand("감자"));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.itemName()).isEqualTo("감자");
            assertThat(result.productName()).isEqualTo("감자 5kg");
            assertThat(result.price()).isEqualByComparingTo("178");
            assertThat(result.unit()).isEqualTo(OnlinePriceCrawlResult.PER_100_GRAMS);
            assertThat(result.productUrl()).isEqualTo(product.productUrl());
            assertThat(result.deliveryNote()).isEqualTo("무료배송");
        });
    }
}
