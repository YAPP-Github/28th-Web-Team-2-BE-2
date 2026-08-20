package com.example.demo.item.application.query;

import java.time.LocalDate;
import java.time.Period;

/** 공공가격 추이 조회 기간이다. 조회 구간은 {@code (startExclusive, 기준일]}로 해석한다. */
public enum PublicPricePeriod {
    WEEK(Period.ofWeeks(1)),
    MONTH(Period.ofMonths(1)),
    YEAR(Period.ofYears(1));

    private final Period length;

    PublicPricePeriod(final Period length) {
        this.length = length;
    }

    public LocalDate startExclusive(final LocalDate baseDate) {
        return baseDate.minus(length);
    }
}
