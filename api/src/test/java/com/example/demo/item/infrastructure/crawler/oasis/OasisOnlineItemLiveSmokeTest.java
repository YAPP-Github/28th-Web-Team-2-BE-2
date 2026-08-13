package com.example.demo.item.infrastructure.crawler.oasis;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.selenium.config.SeleniumOptions;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.oasis.parser.OasisProductDetailParser;
import com.example.demo.item.infrastructure.crawler.oasis.parser.OasisSearchPageParser;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "oasis.live", matches = "true")
class OasisOnlineItemLiveSmokeTest {

    @Test
    void crawlsOasisSearchPage() {
        final SeleniumDriverFactory driverFactory = new SeleniumDriverFactory(new SeleniumOptions(
                true, Duration.ofSeconds(30), Duration.ofSeconds(10)));
        final OasisOnlineItemCrawler crawler = new OasisOnlineItemCrawler(
                driverFactory,
                new OasisSearchPageParser(),
                new OasisProductDetailParser(),
                new OnlineProductSelectionPolicy());

        final List<OasisProduct> products = crawler.crawl("감자");
        assertThat(products).isNotEmpty();
        assertThat(products).allSatisfy(product -> {
            assertThat(product.externalProductId()).isNotBlank();
            assertThat(product.name()).isNotBlank();
            assertThat(product.productUrl()).hasHost("www.oasis.co.kr");
            assertThat(product.pricePer100g()).isPositive();
            assertThat(product.name()).doesNotContain("스프", "칩", "생수제비", "만두", "샐러드");
        });
    }
}
