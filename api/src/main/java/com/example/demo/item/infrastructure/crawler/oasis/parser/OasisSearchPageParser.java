package com.example.demo.item.infrastructure.crawler.oasis.parser;

import com.example.demo.item.infrastructure.crawler.oasis.OasisProduct;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class OasisSearchPageParser {

    private static final String OASIS_BASE_URL = "https://www.oasis.co.kr";
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("/product/detail/(\\d+)");

    public List<OasisProduct> parse(final String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        return Jsoup.parse(html, OASIS_BASE_URL).select(".wrapBox").stream()
                .map(this::parseProduct)
                .filter(product -> product != null)
                .toList();
    }

    private OasisProduct parseProduct(final Element card) {
        final Element link = card.selectFirst("a[href*='/product/detail/']");
        final Element name = card.selectFirst(".listTit");
        final Element sellingPrice = card.selectFirst(".price_discount b");
        if (link == null || name == null || sellingPrice == null) {
            return null;
        }
        final Matcher idMatcher = PRODUCT_ID_PATTERN.matcher(link.attr("href"));
        if (!idMatcher.find()) {
            return null;
        }
        final URI productUrl = URI.create(link.absUrl("href"));
        final BigDecimal price = parsePrice(sellingPrice.text());
        if (productUrl.getHost() == null || price == null) {
            return null;
        }
        return new OasisProduct(
                idMatcher.group(1),
                name.text().trim(),
                productUrl,
                price,
                parsePrice(text(card, ".price_original b")))
                .withDeliveryNote(parseDeliveryNote(card));
    }

    private String text(final Element element, final String selector) {
        final Element selected = element.selectFirst(selector);
        if (selected == null) {
            return "";
        }
        return selected.text();
    }

    private BigDecimal parsePrice(final String text) {
        final String digits = text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        return new BigDecimal(digits);
    }

    private String parseDeliveryNote(final Element card) {
        final Element delivery = card.selectFirst(".badge_deliveryOasis, [class*='badge_delivery']");
        if (delivery == null) {
            return null;
        }
        return delivery.text().trim();
    }
}
