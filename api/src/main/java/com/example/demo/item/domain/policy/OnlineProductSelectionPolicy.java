package com.example.demo.item.domain.policy;

import java.util.Map;
import java.util.Set;

public class OnlineProductSelectionPolicy {

    private static final Map<String, Set<String>> EXCLUDED_KEYWORDS_BY_ITEM = Map.of(
            normalize("감자"), Set.of(
                    "감자스프", "감자칩", "감자생수제비", "감자만두", "감자샐러드", "감자전분", "감자핫도그", "감자두부"));

    public boolean isTargetProduct(final String itemName, final String productName) {
        final String normalizedItemName = normalize(itemName);
        final String normalizedProductName = normalize(productName);
        final Set<String> excludedKeywords = EXCLUDED_KEYWORDS_BY_ITEM.get(normalizedItemName);
        if (excludedKeywords == null) {
            return true;
        }
        return excludedKeywords.stream().noneMatch(normalizedProductName::contains);
    }

    private static String normalize(final String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "");
    }
}
