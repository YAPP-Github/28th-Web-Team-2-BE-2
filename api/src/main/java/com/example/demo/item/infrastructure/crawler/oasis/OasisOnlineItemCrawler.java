package com.example.demo.item.infrastructure.crawler.oasis;

import com.example.demo.common.exception.ApiException;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.oasis.parser.OasisProductDetailParser;
import com.example.demo.item.infrastructure.crawler.oasis.parser.OasisSearchPageParser;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OasisOnlineItemCrawler {

    private static final String SEARCH_URL = "https://www.oasis.co.kr/product/search?keyword=";
    private static final String SEARCH_OPTIONS = "&page=1&sort=priority";
    private static final int MAX_CRAWL_RESULT_COUNT = 5;
    private final SeleniumDriverFactory driverFactory;
    private final OasisSearchPageParser searchPageParser;
    private final OasisProductDetailParser detailParser;
    private final OnlineProductSelectionPolicy productSelectionPolicy;

    public List<OasisProduct> crawl(final String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw ApiException.invalidParameter();
        }
        final SeleniumPage page = driverFactory.loadPage(searchUrl(itemName), OasisOnlineItemCrawler::hasProductLinks);
        return searchPageParser.parse(page.html()).stream()
                .filter(product -> productSelectionPolicy.isTargetProduct(itemName, product.name()))
                .limit(MAX_CRAWL_RESULT_COUNT)
                .map(this::loadUnitPrice)
                .filter(product -> product.pricePer100g() != null)
                .toList();
    }

    private OasisProduct loadUnitPrice(final OasisProduct product) {
        final SeleniumPage detailPage = driverFactory.loadPage(product.productUrl(), OasisOnlineItemCrawler::hasUnitPrice);
        return product.withPricePer100g(detailParser.parsePricePer100g(detailPage.html()))
                .withDeliveryNote(detailParser.parseDeliveryNote(detailPage.html()));
    }

    private static boolean hasProductLinks(final String pageSource) {
        return pageSource.contains("/product/detail/");
    }

    private static boolean hasUnitPrice(final String pageSource) {
        return pageSource.contains("opt_unit") || pageSource.contains("info_option");
    }

    private URI searchUrl(final String itemName) {
        return URI.create(SEARCH_URL + URLEncoder.encode(itemName, StandardCharsets.UTF_8) + SEARCH_OPTIONS);
    }
}
