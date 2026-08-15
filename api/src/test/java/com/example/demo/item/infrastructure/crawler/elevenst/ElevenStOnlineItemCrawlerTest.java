package com.example.demo.item.infrastructure.crawler.elevenst;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.elevenst.parser.ElevenStProductDetailParser;
import com.example.demo.item.infrastructure.crawler.elevenst.parser.ElevenStSearchPageParser;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ElevenStOnlineItemCrawlerTest {

    @Test
    void returnsTopFiveProductsWithDetailPrice() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final ElevenStSearchPageParser parser = mock(ElevenStSearchPageParser.class);
        final ElevenStProductDetailParser detailParser = mock(ElevenStProductDetailParser.class);
        final URI searchUrl = URI.create(
                "https://apis.11st.co.kr/search/api/tab?poc=PC&tabId=TOTAL_SEARCH&tier=A&searchKeyword=%EA%B0%90%EC%9E%90&pageNo=1");
        final List<ElevenStProduct> products = IntStream.rangeClosed(1, 6)
                .mapToObj(index -> product(String.valueOf(index), "감자 " + index))
                .toList();
        when(driverFactory.loadPage(any(URI.class), any())).thenReturn(new SeleniumPage(searchUrl, "detail"));
        when(driverFactory.loadPage(any(URI.class))).thenReturn(new SeleniumPage(searchUrl, "detail"));
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "search"));
        when(parser.parse("search")).thenReturn(products);
        when(detailParser.parsePricePer100g(any(), any(), any())).thenReturn(BigDecimal.valueOf(890));

        final ElevenStOnlineItemCrawler crawler = new ElevenStOnlineItemCrawler(
                driverFactory, parser, detailParser, new OnlineProductSelectionPolicy());

        assertThat(crawler.crawl("감자")).extracting(ElevenStProduct::externalProductId)
                .containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    void returnsFallbackPriceWhenDetailPageHasNoUnitPrice() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final ElevenStSearchPageParser parser = mock(ElevenStSearchPageParser.class);
        final ElevenStProductDetailParser detailParser = new ElevenStProductDetailParser();
        final URI searchUrl = URI.create(
                "https://apis.11st.co.kr/search/api/tab?poc=PC&tabId=TOTAL_SEARCH&tier=A&searchKeyword=%EA%B0%90%EC%9E%90&pageNo=1");
        final ElevenStProduct product = product("1", "감자 5kg");
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "search"));
        when(driverFactory.loadPage(eq(product.productUrl()))).thenReturn(
                new SeleniumPage(product.productUrl(), "<html><body>상품 상세 페이지</body></html>"));
        when(parser.parse("search")).thenReturn(List.of(product));

        final ElevenStOnlineItemCrawler crawler = new ElevenStOnlineItemCrawler(
                driverFactory, parser, detailParser, new OnlineProductSelectionPolicy());

        assertThat(crawler.crawl("감자")).singleElement().satisfies(result ->
                assertThat(result.pricePer100g()).isEqualByComparingTo("178"));
    }

    private ElevenStProduct product(final String id, final String name) {
        return new ElevenStProduct(
                id, name, URI.create("https://www.11st.co.kr/products/" + id), BigDecimal.valueOf(8900), null);
    }
}
