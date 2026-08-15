package com.example.demo.item.infrastructure.crawler.gsshop;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.selenium.config.SeleniumOptions;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.gsshop.parser.GsShopProductDetailParser;
import com.example.demo.item.infrastructure.crawler.gsshop.parser.GsShopSearchPageParser;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "gsshop.live", matches = "true")
class GsShopOnlineItemLiveSmokeTest {

    @Test
    void crawlsGsShopSearchAndDetails() {
        final SeleniumDriverFactory driverFactory = new SeleniumDriverFactory(new SeleniumOptions(
                true, Duration.ofSeconds(30), Duration.ofSeconds(10)));
        final GsShopOnlineItemCrawler crawler = new GsShopOnlineItemCrawler(
                driverFactory,
                new GsShopSearchPageParser(),
                new GsShopProductDetailParser(),
                new OnlineProductSelectionPolicy());

        final List<GsShopProduct> products = crawler.crawl("감자");

        assertThat(products).isNotEmpty();
        assertThat(products).allSatisfy(product -> {
            assertThat(product.externalProductId()).isNotBlank();
            assertThat(product.name()).isNotBlank();
            assertThat(product.productUrl()).hasHost("www.gsshop.com");
            assertThat(product.sellingPrice()).isPositive();
            assertThat(product.pricePer100g()).isPositive();
        });
    }
}
