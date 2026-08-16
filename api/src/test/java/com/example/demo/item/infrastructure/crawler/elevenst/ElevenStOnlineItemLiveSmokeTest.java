package com.example.demo.item.infrastructure.crawler.elevenst;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.selenium.config.SeleniumOptions;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.elevenst.parser.ElevenStProductDetailParser;
import com.example.demo.item.infrastructure.crawler.elevenst.parser.ElevenStSearchPageParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "elevenst.live", matches = "true")
class ElevenStOnlineItemLiveSmokeTest {

    @Test
    void crawlsElevenStSearchApiAndDetails() throws Exception {
        final SeleniumDriverFactory driverFactory = new SeleniumDriverFactory(new SeleniumOptions(
                true, Duration.ofSeconds(30), Duration.ofSeconds(10)));
        final ElevenStOnlineItemCrawler crawler = new ElevenStOnlineItemCrawler(
                driverFactory,
                new ElevenStSearchPageParser(new ObjectMapper()),
                new ElevenStProductDetailParser(),
                new OnlineProductSelectionPolicy());

        final List<ElevenStProduct> products = crawler.crawl("감자");
        assertThat(products).isNotEmpty();
        assertThat(products).allSatisfy(product -> {
            assertThat(product.externalProductId()).isNotBlank();
            assertThat(product.name()).isNotBlank();
            assertThat(product.productUrl()).hasHost("www.11st.co.kr");
            assertThat(product.sellingPrice()).isPositive();
            assertThat(product.pricePer100g()).isPositive();
            assertThat(product.name()).doesNotContain("스프", "칩", "생수제비", "만두", "샐러드");
        });
    }
}
