package com.example.demo.item.domain.policy;

import java.util.Map;
import java.util.Set;

public class OnlineProductSelectionPolicy {

    private static final Map<String, Set<String>> EXCLUDED_KEYWORDS_BY_ITEM = Map.of(
            "감자", Set.of(
                    "감자스프", "감자칩", "감자 생수제비", "감자만두", "감자 만두", "감자샐러드", "감자 샐러드",
                    "감자전분", "감자 전분", "감자핫도그", "감자 핫도그", "감자 두부"));

    public boolean isTargetProduct(final String itemName, final String productName) {
        final Set<String> excludedKeywords = EXCLUDED_KEYWORDS_BY_ITEM.get(itemName);
        if (excludedKeywords == null) {
            return true;
        }
        return excludedKeywords.stream().noneMatch(productName::contains);
    }
}
