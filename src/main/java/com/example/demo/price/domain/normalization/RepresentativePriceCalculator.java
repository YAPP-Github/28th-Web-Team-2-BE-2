package com.example.demo.price.domain.normalization;

import com.example.demo.price.domain.NormalizedPrice;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
public class RepresentativePriceCalculator {

    private static final int MINIMUM_SAMPLE_COUNT = 3;

    public Optional<NormalizedPrice> calculate(final List<NormalizedPrice> prices) {
        if (prices == null || prices.size() < MINIMUM_SAMPLE_COUNT) {
            return Optional.empty();
        }
        final List<NormalizedPrice> sorted = prices.stream()
                .sorted(Comparator.comparing(NormalizedPrice::amount))
                .toList();
        final int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return Optional.of(sorted.get(middle));
        }
        final BigDecimal median = sorted.get(middle - 1).amount()
                .add(sorted.get(middle).amount())
                .divide(BigDecimal.valueOf(2));
        return Optional.of(new NormalizedPrice(median, sorted.get(middle).unit()));
    }
}
