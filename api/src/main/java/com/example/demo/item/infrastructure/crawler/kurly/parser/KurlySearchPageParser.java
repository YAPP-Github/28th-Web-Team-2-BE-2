package com.example.demo.item.infrastructure.crawler.kurly.parser;

import com.example.demo.item.infrastructure.crawler.kurly.KurlyProduct;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class KurlySearchPageParser {

    private static final String KURLY_BASE_URL = "https://www.kurly.com";
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("/goods/(\\d+)");

    public List<KurlyProduct> parse(final String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        final Document document = Jsoup.parse(html, KURLY_BASE_URL);
        return document.select("a[href*=/goods/]").stream()
                .map(this::parseProduct)
                .filter(product -> product != null)
                .toList();
    }

    private KurlyProduct parseProduct(final Element card) {
        final String productUrl = card.absUrl("href");
        final String productId = productId(productUrl);
        final Element sellingPriceElement = card.selectFirst(".sales-price .price-number, .sales-price");
        final Element nameElement = productName(card);
        if (productId == null || nameElement == null || nameElement.text().isBlank() || sellingPriceElement == null) {
            return null;
        }

        final BigDecimal sellingPrice = price(sellingPriceElement.text());
        if (sellingPrice == null) {
            return null;
        }
        return new KurlyProduct(
                productId,
                nameElement.text().trim(),
                URI.create(productUrl),
                sellingPrice,
                price(card.select(".dimmed-price .price-number").text()))
                .withDeliveryNote(deliveryNote(card));
    }

    private String deliveryNote(final Element card) {
        final Element deliveryElement = card.selectFirst(".delivery, [class*='delivery']");
        if (deliveryElement == null || deliveryElement.text().isBlank()) {
            return null;
        }
        return deliveryElement.text().trim();
    }

    private Element productName(final Element card) {
        final Element knownName = card.selectFirst(".product-name, [class*='product-name']");
        if (knownName != null) {
            return knownName;
        }

        final Element discountPrice = card.selectFirst(".discount-price");
        if (discountPrice == null || discountPrice.parent() == null) {
            return null;
        }

        final List<Element> siblings = discountPrice.parent().children();
        final int discountPriceIndex = siblings.indexOf(discountPrice);
        for (int index = discountPriceIndex - 1; index >= 0; index--) {
            final Element sibling = siblings.get(index);
            if (sibling.tagName().equals("span") && !sibling.text().isBlank()) {
                return sibling;
            }
        }
        return null;
    }

    private String productId(final String productUrl) {
        final Matcher matcher = PRODUCT_ID_PATTERN.matcher(productUrl);
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
