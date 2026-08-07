package com.example.demo.kamis.application.query;

import java.time.LocalDate;

public record KamisDailyPriceQuery(
        String productClsCode,
        String itemCategoryCode,
        String countryCode,
        LocalDate regDay,
        String convertKgYn) {}
