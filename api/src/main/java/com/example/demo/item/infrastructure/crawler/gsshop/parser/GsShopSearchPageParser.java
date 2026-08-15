package com.example.demo.item.infrastructure.crawler.gsshop.parser;

import com.example.demo.item.infrastructure.crawler.gsshop.GsShopProduct;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class GsShopSearchPageParser {

    private static final String BASE_URL = "https://www.gsshop.com";
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("(?:^|[?&])prdid=(\\d+)");

    public List<GsShopProduct> parse(final String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        return Jsoup.parse(html, BASE_URL).select("a.prd-item[data-prdid]").stream()
                .map(this::parseProduct)
                .filter(product -> product != null)
                .toList();
    }

    private GsShopProduct parseProduct(final Element card) {
        final String productId = productId(card);
        final Element name = card.selectFirst(".prd-name");
        final Element price = card.selectFirst(".set-price strong");
        if (productId == null || name == null || name.text().isBlank() || price == null) {
            return null;
        }
        final BigDecimal sellingPrice = price(price.text());
        if (sellingPrice == null) {
            return null;
        }
        return new GsShopProduct(
                productId,
                name.text().trim(),
                URI.create(BASE_URL + "/prd/prd.gs?prdid=" + productId),
                sellingPrice,
                null);
    }

    private String productId(final Element card) {
        final String dataProductId = card.attr("data-prdid");
        if (!dataProductId.isBlank() && dataProductId.matches("\\d+")) {
            return dataProductId;
        }
        final Matcher matcher = PRODUCT_ID_PATTERN.matcher(card.attr("href"));
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private BigDecimal price(final String text) {
        final String digits = text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        return new BigDecimal(digits);
    }
}
