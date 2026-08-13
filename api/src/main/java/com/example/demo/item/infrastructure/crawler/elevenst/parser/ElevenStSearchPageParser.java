package com.example.demo.item.infrastructure.crawler.elevenst.parser;

import com.example.demo.item.infrastructure.crawler.elevenst.ElevenStProduct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class ElevenStSearchPageParser {

    private static final String PRODUCT_URL = "https://www.11st.co.kr/products/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ElevenStProduct> parse(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return parseProducts(objectMapper.readTree(jsonContent(json)));
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String jsonContent(final String source) {
        if (source.trim().startsWith("{")) {
            return source;
        }
        return Jsoup.parse(source).body().text();
    }

    private List<ElevenStProduct> parseProducts(final JsonNode root) {
        final List<ElevenStProduct> products = new ArrayList<>();
        for (JsonNode group : root.path("data")) {
            for (JsonNode item : group.path("items")) {
                final ElevenStProduct product = parseProduct(item);
                if (product != null) {
                    products.add(product);
                }
            }
        }
        return List.copyOf(products);
    }

    private ElevenStProduct parseProduct(final JsonNode item) {
        final String id = text(item, "id");
        final String name = text(item, "title");
        final BigDecimal sellingPrice = decimal(item, "finalPrc");
        if (id.isBlank() || name.isBlank() || sellingPrice == null) {
            return null;
        }
        return new ElevenStProduct(
                id,
                name.trim(),
                URI.create(PRODUCT_URL + id),
                sellingPrice,
                decimal(item.path("maxDiscountInfo"), "sellPrice"))
                .withDeliveryNote(textOrNull(item, "deliveryDescription"));
    }

    private String text(final JsonNode node, final String fieldName) {
        return node.path(fieldName).asText("");
    }

    private String textOrNull(final JsonNode node, final String fieldName) {
        final String value = text(node, fieldName).trim();
        if (value.isBlank()) {
            return null;
        }
        return value;
    }

    private BigDecimal decimal(final JsonNode node, final String fieldName) {
        final JsonNode value = node.path(fieldName);
        if (!value.isNumber() && !value.isTextual()) {
            return null;
        }
        final String text = value.asText().replaceAll("[^0-9.]", "");
        if (text.isBlank()) {
            return null;
        }
        return new BigDecimal(text);
    }
}
