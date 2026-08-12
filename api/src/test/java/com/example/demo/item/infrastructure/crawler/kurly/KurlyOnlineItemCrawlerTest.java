package com.example.demo.item.infrastructure.crawler.kurly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.kurly.parser.KurlyProductDetailParser;
import com.example.demo.item.infrastructure.crawler.kurly.parser.KurlySearchPageParser;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class KurlyOnlineItemCrawlerTest {

    @Test
    void searchesKurlyWithItemNameAndParsesRenderedPage() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final KurlySearchPageParser parser = mock(KurlySearchPageParser.class);
        final KurlyProductDetailParser detailParser = mock(KurlyProductDetailParser.class);
        final URI searchUrl = URI.create("https://www.kurly.com/search?sword=%EA%B0%90%EC%9E%90&site=market");
        final KurlyProduct product = new KurlyProduct(
                "5026448", "[KF365] 감자 1kg", URI.create("https://www.kurly.com/goods/5026448"),
                BigDecimal.valueOf(2990), BigDecimal.valueOf(4990));
        when(driverFactory.loadPage(any(URI.class), any())).thenReturn(new SeleniumPage(searchUrl, "<html>detail</html>"));
        when(detailParser.parsePricePer100g("<html>detail</html>")).thenReturn(BigDecimal.valueOf(399));
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "<html>items</html>"));
        when(driverFactory.loadPage(eq(product.productUrl()), any()))
                .thenReturn(new SeleniumPage(product.productUrl(), "<html>detail</html>"));
        when(parser.parse("<html>items</html>")).thenReturn(List.of(product));
        final KurlyOnlineItemCrawler crawler = crawler(driverFactory, parser, detailParser);

        final List<KurlyProduct> result = crawler.crawl("감자");

        assertThat(result).containsExactly(product.withPricePer100g(BigDecimal.valueOf(399)));
        verify(driverFactory).loadPage(eq(searchUrl), any());
        verify(parser).parse("<html>items</html>");
    }

    @Test
    void returnsTopFiveProductsInSearchOrder() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final KurlySearchPageParser parser = mock(KurlySearchPageParser.class);
        final KurlyProductDetailParser detailParser = mock(KurlyProductDetailParser.class);
        final URI searchUrl = URI.create("https://www.kurly.com/search?sword=%EA%B0%90%EC%9E%90&site=market");
        final List<KurlyProduct> products = IntStream.rangeClosed(1, 6)
                .mapToObj(index -> new KurlyProduct(
                        String.valueOf(index), "상품 " + index,
                        URI.create("https://www.kurly.com/goods/" + index),
                        BigDecimal.valueOf(index * 100L), null))
                .toList();
        when(driverFactory.loadPage(any(URI.class), any())).thenReturn(new SeleniumPage(searchUrl, "<html>detail</html>"));
        when(detailParser.parsePricePer100g("<html>detail</html>")).thenReturn(BigDecimal.valueOf(399));
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "<html>items</html>"));
        when(parser.parse("<html>items</html>")).thenReturn(products);
        final KurlyOnlineItemCrawler crawler = crawler(driverFactory, parser, detailParser);

        assertThat(crawler.crawl("감자")).extracting(KurlyProduct::externalProductId)
                .containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    void returnsTopFiveRawPotatoProductsAndExcludesProcessedFoods() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final KurlySearchPageParser parser = mock(KurlySearchPageParser.class);
        final KurlyProductDetailParser detailParser = mock(KurlyProductDetailParser.class);
        final URI searchUrl = URI.create("https://www.kurly.com/search?sword=%EA%B0%90%EC%9E%90&site=market");
        final List<KurlyProduct> products = List.of(
                product("1", "감자스프 4종", 2890),
                product("2", "[KF365] 감자 1kg", 3990),
                product("3", "[팜송] 감자 2kg", 6390),
                product("4", "감자 생수제비 300g", 2670),
                product("5", "골든킹 감자 900g", 3990),
                product("6", "친환경 깐 감자 300g", 2490),
                product("7", "[팜송] 한끼 감자 300g", 2490));
        when(driverFactory.loadPage(any(URI.class), any())).thenReturn(new SeleniumPage(searchUrl, "<html>detail</html>"));
        when(detailParser.parsePricePer100g("<html>detail</html>")).thenReturn(BigDecimal.valueOf(399));
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "<html>items</html>"));
        when(parser.parse("<html>items</html>")).thenReturn(products);
        final KurlyOnlineItemCrawler crawler = crawler(driverFactory, parser, detailParser);

        assertThat(crawler.crawl("감자")).extracting(KurlyProduct::externalProductId)
                .containsExactly("2", "3", "5", "6", "7");
    }

    private KurlyProduct product(final String id, final String name, final int price) {
        return new KurlyProduct(
                id,
                name,
                URI.create("https://www.kurly.com/goods/" + id),
                BigDecimal.valueOf(price),
                null);
    }

    private KurlyOnlineItemCrawler crawler(
            final SeleniumDriverFactory driverFactory,
            final KurlySearchPageParser parser,
            final KurlyProductDetailParser detailParser) {
        return new KurlyOnlineItemCrawler(
                driverFactory, parser, detailParser, new OnlineProductSelectionPolicy());
    }
}
