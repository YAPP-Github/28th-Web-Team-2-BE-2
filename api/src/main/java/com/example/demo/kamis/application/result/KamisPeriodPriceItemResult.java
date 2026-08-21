package com.example.demo.kamis.application.result;

public record KamisPeriodPriceItemResult(
        String itemName,
        String kindName,
        String countyName,
        String marketName,
        String year,
        String regDay,
        String price,
        String unit) {}
