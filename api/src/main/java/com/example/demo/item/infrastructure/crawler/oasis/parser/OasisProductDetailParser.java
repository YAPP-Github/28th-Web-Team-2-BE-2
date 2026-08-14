package com.example.demo.item.infrastructure.crawler.oasis.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class OasisProductDetailParser {

    private static final BigDecimal HUNDRED_GRAMS = BigDecimal.valueOf(100);
    private static final BigDecimal GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1_000);
    private static final Pattern UNIT_PRICE_PATTERN = Pattern.compile(
            "([0-9]+(?:\\.[0-9]+)?)\\s*(g|kg)\\s*당\\s*([0-9,]+)\\s*원");

    public BigDecimal parsePricePer100g(final String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return Jsoup.parse(html).select(".opt_unit, .info_option").stream()
                .map(Element::text)
                .map(this::parseUnitPrice)
                .filter(price -> price != null)
                .findFirst()
                .orElse(null);
    }

    public String parseDeliveryNote(final String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return Jsoup.parse(html).select(".badge_deliveryOasis, [class*='badge_delivery']").stream()
                .map(Element::text)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElse(null);
    }

    private BigDecimal parseUnitPrice(final String text) {
        final Matcher matcher = UNIT_PRICE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        final BigDecimal unit = new BigDecimal(matcher.group(1));
        final BigDecimal price = new BigDecimal(matcher.group(3).replace(",", ""));
        final BigDecimal grams = grams(matcher.group(2), unit);
        return price.multiply(HUNDRED_GRAMS).divide(grams, 0, RoundingMode.HALF_UP);
    }

    private BigDecimal grams(final String unitName, final BigDecimal unit) {
        if ("kg".equals(unitName)) {
            return unit.multiply(GRAMS_PER_KILOGRAM);
        }
        return unit;
    }
}
