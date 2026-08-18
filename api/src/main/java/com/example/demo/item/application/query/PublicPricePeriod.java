package com.example.demo.item.application.query;

import java.time.LocalDate;

/** 공공가격 추이 조회 기간이다. 조회 구간은 {@code (startExclusive, 기준일]}로 해석한다. */
public enum PublicPricePeriod {
    WEEK,
    MONTH,
    YEAR;

    public LocalDate startExclusive(final LocalDate baseDate) {
        if (this == WEEK) {
            return baseDate.minusWeeks(1);
        }
        if (this == MONTH) {
            return baseDate.minusMonths(1);
        }
        return baseDate.minusYears(1);
    }
}
