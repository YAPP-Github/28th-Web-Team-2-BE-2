package com.example.demo.item.application.query;

import java.time.LocalDate;

/** 공공가격 조회 구간이다. {@code (startExclusive, endInclusive]}로 해석한다. */
public record PublicPriceRange(LocalDate startExclusive, LocalDate endInclusive) {

    public static PublicPriceRange of(final PublicPricePeriod period, final LocalDate baseDate) {
        return new PublicPriceRange(period.startExclusive(baseDate), baseDate);
    }
}
