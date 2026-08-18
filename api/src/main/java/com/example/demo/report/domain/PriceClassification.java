package com.example.demo.report.domain;

/** 제보 가격을 제보 당시 공공가격과 비교한 분류다. */
public enum PriceClassification {
    CHEAP,
    EXPENSIVE,
    EQUAL;

    /** 비교값이 없으면 분류하지 않고 {@code null}을 반환한다. */
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
