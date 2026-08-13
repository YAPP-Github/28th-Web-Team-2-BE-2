package com.example.demo.item.infrastructure.crawler.elevenst;

import com.example.demo.common.exception.ApiException;
import com.example.demo.external.selenium.SeleniumPage;
import com.example.demo.external.selenium.factory.SeleniumDriverFactory;
import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import com.example.demo.item.infrastructure.crawler.elevenst.parser.ElevenStProductDetailParser;
import com.example.demo.item.infrastructure.crawler.elevenst.parser.ElevenStSearchPageParser;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ElevenStOnlineItemCrawler {

    private static final String SEARCH_URL = "https://apis.11st.co.kr/search/api/tab?poc=PC&tabId=TOTAL_SEARCH&tier=A&searchKeyword=";
    private static final String SEARCH_OPTIONS = "&pageNo=1";
    private static final int MAX_CRAWL_RESULT_COUNT = 5;
    private final SeleniumDriverFactory driverFactory;
    private final ElevenStSearchPageParser searchPageParser;
    private final ElevenStProductDetailParser detailParser;
    private final OnlineProductSelectionPolicy productSelectionPolicy;

    public List<ElevenStProduct> crawl(final String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw ApiException.invalidParameter();
        }
        final SeleniumPage searchPage = driverFactory.loadPage(searchUrl(itemName), ElevenStOnlineItemCrawler::hasSearchResults);
        return searchPageParser.parse(searchPage.html()).stream()
                .filter(product -> productSelectionPolicy.isTargetProduct(itemName, product.name()))
                .limit(MAX_CRAWL_RESULT_COUNT)
                .map(this::loadUnitPrice)
                .filter(product -> product.pricePer100g() != null)
                .toList();
    }

    private ElevenStProduct loadUnitPrice(final ElevenStProduct product) {
        final SeleniumPage detailPage = driverFactory.loadPage(product.productUrl(), ElevenStOnlineItemCrawler::hasUnitPrice);
        final String deliveryNote = detailParser.parseDeliveryNote(detailPage.html());
        final ElevenStProduct priceUpdatedProduct = product.withPricePer100g(
                detailParser.parsePricePer100g(detailPage.html(), product.name(), product.sellingPrice()));
        if (deliveryNote == null) {
            return priceUpdatedProduct;
        }
        return priceUpdatedProduct.withDeliveryNote(deliveryNote);
    }

    private static boolean hasSearchResults(final String pageSource) {
        return pageSource.contains("\"data\"") && pageSource.contains("\"items\"");
    }

    private static boolean hasUnitPrice(final String pageSource) {
        return pageSource.contains("pricePerUnitResult") || pageSource.contains("price_per_unit");
    }

    private URI searchUrl(final String itemName) {
        return URI.create(SEARCH_URL + URLEncoder.encode(itemName, StandardCharsets.UTF_8) + SEARCH_OPTIONS);
    }
}
