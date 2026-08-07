package com.example.demo.price.domain.matching;

import com.example.demo.price.domain.RawOffer;
import java.util.Locale;
import java.util.Set;
public class ProductMatcher {

    private static final Set<String> EXCLUDED_KEYWORDS = Set.of(
            "씨앗", "칩", "튀김", "전분", "분말", "냉동", "샐러드", "혼합");

    public boolean matches(final String itemName, final RawOffer offer) {
        final String title = offer.title().toLowerCase(Locale.ROOT);
        final String normalizedItemName = itemName.toLowerCase(Locale.ROOT);
        if (!title.contains(normalizedItemName)) {
            return false;
        }
        if (EXCLUDED_KEYWORDS.stream().anyMatch(title::contains)) {
            return false;
        }
        return offer.available() && !offer.advertisement();
    }

}
