package com.example.demo.item.infrastructure.crawler.kurly.parser;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class KurlyProductDetailParser {

    private static final String UNIT_PRICE_LABEL = "단위 당 가격";
    private static final String DELIVERY_NOTE = "샛별배송";
    private static final Pattern PRICE_PATTERN = Pattern.compile("100g\\s*당\\s*([0-9,]+)원");

    public BigDecimal parsePricePer100g(final String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        final Element label = Jsoup.parse(html).select("dt").stream()
                .filter(element -> element.text().equals(UNIT_PRICE_LABEL))
                .findFirst()
                .orElse(null);
        if (label == null || label.parent() == null) {
            return null;
        }
        return parsePrice(label.parent().text());
    }

    public String parseDeliveryNote(final String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        final Document document = Jsoup.parse(html);
        return document.getElementsContainingOwnText(DELIVERY_NOTE).stream()
                .map(Element::text)
                .map(String::trim)
                .filter(DELIVERY_NOTE::equals)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal parsePrice(final String text) {
        final Matcher matcher = PRICE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1).replace(",", ""));
    }
}
