package com.example.demo.item.infrastructure.crawler.gsshop;

import com.example.demo.common.exception.ApiException;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.gsshop.parser.GsShopProductDetailParser;
import com.example.demo.item.infrastructure.crawler.gsshop.parser.GsShopSearchPageParser;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GsShopOnlineItemCrawler {

    private static final String SEARCH_URL = "https://www.gsshop.com/shop/search/main.gs?tq=";
    private static final int MAX_CRAWL_RESULT_COUNT = 5;
    private final SeleniumDriverFactory driverFactory;
    private final GsShopSearchPageParser searchPageParser;
    private final GsShopProductDetailParser detailParser;
    private final OnlineProductSelectionPolicy productSelectionPolicy;

    public List<GsShopProduct> crawl(final String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw ApiException.invalidParameter();
        }
        final SeleniumPage searchPage = driverFactory.loadPage(searchUrl(itemName), GsShopOnlineItemCrawler::hasSearchResults);
        return searchPageParser.parse(searchPage.html()).stream()
                .filter(product -> productSelectionPolicy.isTargetProduct(itemName, product.name()))
                .limit(MAX_CRAWL_RESULT_COUNT)
                .map(this::loadPrice)
                .filter(product -> product.pricePer100g() != null)
                .toList();
    }

    private GsShopProduct loadPrice(final GsShopProduct product) {
        final SeleniumPage detailPage = driverFactory.loadPage(product.productUrl());
        final GsShopProduct priceUpdatedProduct = product.withPricePer100g(
                detailParser.parsePricePer100g(detailPage.html(), product.name(), product.sellingPrice()));
        final String deliveryNote = detailParser.parseDeliveryNote(detailPage.html());
        if (deliveryNote == null) {
            return priceUpdatedProduct;
        }
        return priceUpdatedProduct.withDeliveryNote(deliveryNote);
    }

    private static boolean hasSearchResults(final String pageSource) {
        return !Jsoup.parse(pageSource).select("a.prd-item[data-prdid]").isEmpty();
    }

    private URI searchUrl(final String itemName) {
        return URI.create(SEARCH_URL + URLEncoder.encode(itemName, StandardCharsets.UTF_8));
    }
}
