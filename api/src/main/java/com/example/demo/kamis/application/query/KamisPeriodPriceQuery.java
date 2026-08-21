package com.example.demo.kamis.application.query;

import java.time.LocalDate;

public record KamisPeriodPriceQuery(
        String itemCategoryCode,
        String itemCode,
        String kindCode,
        String productRankCode,
        String countryCode,
        LocalDate startDay,
        LocalDate endDay,
        String convertKgYn) {}
