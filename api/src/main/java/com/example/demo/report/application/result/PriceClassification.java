package com.example.demo.report.application.result;

public enum PriceClassification {
    CHEAP,
    EXPENSIVE,
    EQUAL;

    public static PriceClassification of(final Integer priceGap) {
        if (priceGap == null) {
            return null;
        }
        if (priceGap < 0) {
            return CHEAP;
        }
        if (priceGap > 0) {
            return EXPENSIVE;
        }
        return EQUAL;
    }
}
