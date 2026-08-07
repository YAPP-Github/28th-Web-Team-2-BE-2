package com.example.demo.external.kamis;

import java.time.LocalDate;

public record KamisDailyPriceRequest(
        String productClsCode,
        String itemCategoryCode,
        String countryCode,
        LocalDate regDay,
        String convertKgYn) {}
