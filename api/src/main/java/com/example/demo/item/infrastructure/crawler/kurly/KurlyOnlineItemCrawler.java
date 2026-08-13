package com.example.demo.item.infrastructure.crawler.kurly;

import com.example.demo.common.exception.ApiException;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.kurly.parser.KurlyProductDetailParser;
import com.example.demo.item.infrastructure.crawler.kurly.parser.KurlySearchPageParser;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KurlyOnlineItemCrawler {

    private static final String SEARCH_URL = "https://www.kurly.com/search?sword=";
    private static final int MAX_CRAWL_RESULT_COUNT = 5;
    private final SeleniumDriverFactory driverFactory;
    private final KurlySearchPageParser searchPageParser;
    private final KurlyProductDetailParser detailParser;
    private final OnlineProductSelectionPolicy productSelectionPolicy;

    public List<KurlyProduct> crawl(final String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw ApiException.invalidParameter();
        }
        final SeleniumPage page = driverFactory.loadPage(searchUrl(itemName), KurlyOnlineItemCrawler::hasProductLinks);
        return searchPageParser.parse(page.html()).stream()
                .filter(product -> productSelectionPolicy.isTargetProduct(itemName, product.name()))
                .limit(MAX_CRAWL_RESULT_COUNT)
                .map(this::loadUnitPrice)
                .filter(product -> product.pricePer100g() != null)
                .toList();
    }

    private KurlyProduct loadUnitPrice(final KurlyProduct product) {
        final SeleniumPage detailPage = driverFactory.loadPage(product.productUrl(), KurlyOnlineItemCrawler::hasUnitPrice);
        return product.withPricePer100g(detailParser.parsePricePer100g(detailPage.html()))
                .withDeliveryNote(detailParser.parseDeliveryNote(detailPage.html()));
    }

    private static boolean hasProductLinks(final String pageSource) {
        return pageSource.contains("/goods/");
    }

    private static boolean hasUnitPrice(final String pageSource) {
        return pageSource.contains("단위 당 가격") || pageSource.contains("unitPriceText");
    }

    private URI searchUrl(final String itemName) {
        return URI.create(SEARCH_URL + URLEncoder.encode(itemName, StandardCharsets.UTF_8) + "&site=market");
    }
}
