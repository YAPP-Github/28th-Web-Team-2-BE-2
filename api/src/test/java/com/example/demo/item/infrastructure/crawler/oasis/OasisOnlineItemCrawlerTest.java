package com.example.demo.item.infrastructure.crawler.oasis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.oasis.parser.OasisProductDetailParser;
import com.example.demo.item.infrastructure.crawler.oasis.parser.OasisSearchPageParser;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class OasisOnlineItemCrawlerTest {

    @Test
    void returnsTopFiveTargetProductsWithDetailPrice() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final OasisSearchPageParser parser = mock(OasisSearchPageParser.class);
        final OasisProductDetailParser detailParser = mock(OasisProductDetailParser.class);
        final URI searchUrl = URI.create(
                "https://www.oasis.co.kr/product/search?keyword=%EA%B0%90%EC%9E%90&page=1&sort=priority");
        final List<OasisProduct> products = IntStream.rangeClosed(1, 6)
                .mapToObj(index -> product(String.valueOf(index), "감자 " + index, index * 100))
                .toList();
        when(driverFactory.loadPage(any(URI.class), any())).thenReturn(new SeleniumPage(searchUrl, "detail"));
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "items"));
        when(parser.parse("items")).thenReturn(products);
        when(detailParser.parsePricePer100g("detail")).thenReturn(BigDecimal.valueOf(399));

        final OasisOnlineItemCrawler crawler = new OasisOnlineItemCrawler(
                driverFactory, parser, detailParser, new OnlineProductSelectionPolicy());

        assertThat(crawler.crawl("감자")).extracting(OasisProduct::externalProductId)
                .containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    void excludesProcessedProductsBeforeCrawlingDetails() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final OasisSearchPageParser parser = mock(OasisSearchPageParser.class);
        final OasisProductDetailParser detailParser = mock(OasisProductDetailParser.class);
        final URI searchUrl = URI.create(
                "https://www.oasis.co.kr/product/search?keyword=%EA%B0%90%EC%9E%90&page=1&sort=priority");
        when(driverFactory.loadPage(any(URI.class), any())).thenReturn(new SeleniumPage(searchUrl, "detail"));
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "items"));
        when(parser.parse("items")).thenReturn(List.of(
                product("1", "감자스프", 100), product("2", "감자 1kg", 100)));
        when(detailParser.parsePricePer100g("detail")).thenReturn(BigDecimal.valueOf(399));

        final OasisOnlineItemCrawler crawler = new OasisOnlineItemCrawler(
                driverFactory, parser, detailParser, new OnlineProductSelectionPolicy());

        assertThat(crawler.crawl("감자")).extracting(OasisProduct::externalProductId).containsExactly("2");
    }

    private OasisProduct product(final String id, final String name, final int price) {
        return new OasisProduct(
                id, name, URI.create("https://www.oasis.co.kr/product/detail/" + id), BigDecimal.valueOf(price), null);
    }
}
