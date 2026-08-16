package com.example.demo.item.infrastructure.crawler.gsshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.gsshop.parser.GsShopProductDetailParser;
import com.example.demo.item.infrastructure.crawler.gsshop.parser.GsShopSearchPageParser;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class GsShopOnlineItemCrawlerTest {

    @Test
    void returnsFallbackPriceWhenDetailPageHasNoUnitPrice() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final GsShopSearchPageParser parser = mock(GsShopSearchPageParser.class);
        final GsShopProductDetailParser detailParser = new GsShopProductDetailParser();
        final URI searchUrl = URI.create(
                "https://www.gsshop.com/shop/search/main.gs?tq=%EA%B0%90%EC%9E%90");
        final GsShopProduct product = product("57668979", "감자 5kg");
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "search"));
        when(driverFactory.loadPage(eq(product.productUrl()))).thenReturn(
                new SeleniumPage(product.productUrl(), "<html><body>상품 상세 페이지</body></html>"));
        when(parser.parse("search")).thenReturn(List.of(product));

        final GsShopOnlineItemCrawler crawler = new GsShopOnlineItemCrawler(
                driverFactory, parser, detailParser, new OnlineProductSelectionPolicy());

        assertThat(crawler.crawl("감자")).singleElement().satisfies(result ->
                assertThat(result.pricePer100g()).isEqualByComparingTo("178"));
    }

    @Test
    void appliesDeliveryNoteFromDetailPage() {
        final SeleniumDriverFactory driverFactory = mock(SeleniumDriverFactory.class);
        final GsShopSearchPageParser parser = mock(GsShopSearchPageParser.class);
        final GsShopProductDetailParser detailParser = new GsShopProductDetailParser();
        final URI searchUrl = URI.create("https://www.gsshop.com/shop/search/main.gs?tq=%EA%B0%90%EC%9E%90");
        final GsShopProduct product = product("57668979", "감자 5kg");
        when(driverFactory.loadPage(eq(searchUrl), any())).thenReturn(new SeleniumPage(searchUrl, "search"));
        when(driverFactory.loadPage(eq(product.productUrl()))).thenReturn(new SeleniumPage(
                product.productUrl(), "<div class='delivery'>무료배송</div>"));
        when(parser.parse("search")).thenReturn(List.of(product));

        final GsShopOnlineItemCrawler crawler = new GsShopOnlineItemCrawler(
                driverFactory, parser, detailParser, new OnlineProductSelectionPolicy());

        assertThat(crawler.crawl("감자")).singleElement()
                .extracting(GsShopProduct::deliveryNote)
                .isEqualTo("무료배송");
    }

    private GsShopProduct product(final String id, final String name) {
        return new GsShopProduct(
                id, name, URI.create("https://www.gsshop.com/prd/prd.gs?prdid=" + id), BigDecimal.valueOf(8900), null);
    }
}
