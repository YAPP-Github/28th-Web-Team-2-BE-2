package com.example.demo.item.infrastructure.crawler.kurly;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.selenium.config.SeleniumOptions;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.kurly.parser.KurlyProductDetailParser;
import com.example.demo.item.infrastructure.crawler.kurly.parser.KurlySearchPageParser;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "kurly.live", matches = "true")
class KurlyOnlineItemLiveSmokeTest {

    @Test
    void crawlsKurlySearchPage() {
        final SeleniumDriverFactory driverFactory = new SeleniumDriverFactory(new SeleniumOptions(
                true, Duration.ofSeconds(30), Duration.ofSeconds(10)));
        final KurlyOnlineItemCrawler crawler = new KurlyOnlineItemCrawler(
                driverFactory,
                new KurlySearchPageParser(),
                new KurlyProductDetailParser(),
                new OnlineProductSelectionPolicy());

        final List<KurlyProduct> products = crawler.crawl("감자");
        assertThat(products).isNotEmpty();
        assertThat(products).allSatisfy(product -> {
            assertThat(product.externalProductId()).isNotBlank();
            assertThat(product.name()).isNotBlank();
            assertThat(product.productUrl()).hasHost("www.kurly.com");
            assertThat(product.sellingPrice()).isPositive();
            assertThat(product.pricePer100g()).isPositive();
            assertThat(product.deliveryNote()).isNotBlank();
            assertThat(product.name()).doesNotContain("스프", "칩", "생수제비", "만두", "샐러드");
        });
    }
}
